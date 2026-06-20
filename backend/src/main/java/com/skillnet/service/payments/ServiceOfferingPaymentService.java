package com.skillnet.service.payments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillnet.api.dto.payments.CheckoutPaymentResponseDTO;
import com.skillnet.domain.AuditAction;
import com.skillnet.persistence.entity.core.InfoproductorServiceOffering;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.entity.payments.Payment;
import com.skillnet.persistence.repository.InfoproductorServiceOfferingRepository;
import com.skillnet.persistence.repository.PaymentRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.service.AuditService;
import com.skillnet.service.entitlement.ServiceEntitlementService;
import com.skillnet.service.notification.NotificationPublisher;
import com.skillnet.web.dto.request.ServiceOfferingCheckoutRequestDTO;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ServiceOfferingPaymentService {

    private static final Logger log = LoggerFactory.getLogger(ServiceOfferingPaymentService.class);

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final InfoproductorServiceOfferingRepository serviceOfferingRepository;
    private final ServiceEntitlementService entitlementService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final NotificationPublisher notificationPublisher;

    @Value("${stripe.api.secretKey:}")
    private String stripeSecretKey;

    @Value("${skillnet.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @PostConstruct
    public void init() {
        if (stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            Stripe.apiKey = stripeSecretKey;
        }
    }

    @Transactional
    public CheckoutPaymentResponseDTO purchasePlan(ServiceOfferingCheckoutRequestDTO request, Long userId) {
        if (request.getServiceOfferingId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "serviceOfferingId es obligatorio");
        }
        if (request.getAmount() == null || request.getAmount().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount inválido");
        }

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        InfoproductorServiceOffering offering = serviceOfferingRepository
                .findByIdAndActiveTrue(request.getServiceOfferingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan no encontrado o inactivo"));

        BigDecimal expected = offering.getPriceUsd().setScale(2, RoundingMode.HALF_UP);
        BigDecimal requested = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (requested.compareTo(expected) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El monto no coincide con el precio del plan (" + expected + " USD).");
        }

        Instant now = Instant.now();
        Payment payment;
        String message;

        if (expected.signum() == 0) {
            payment = buildServicePayment(user, offering, expected, "SUCCEEDED", "free_plan", "free_" + java.util.UUID.randomUUID(), now);
            payment.setGatewayResponse(objectMapper.createObjectNode().put("mode", "free_plan"));
            paymentRepository.save(payment);
            entitlementService.grantFromPayment(payment);
            notifyPlanPurchase(user, offering);
            message = "Plan activado correctamente.";
        } else if (!isStripeConfigured()) {
            log.warn("STRIPE_SECRET_KEY no configurada: pago simulado del plan {}", offering.getId());
            payment = buildServicePayment(
                    user, offering, expected, "SUCCEEDED", "stripe_dev", "dev_plan_" + java.util.UUID.randomUUID(), now);
            payment.setGatewayResponse(objectMapper.createObjectNode().put("mode", "dev_mock_plan"));
            paymentRepository.save(payment);
            entitlementService.grantFromPayment(payment);
            notifyPlanPurchase(user, offering);
            message = "Plan activado (pago simulado en desarrollo).";
        } else {
            if (request.getPaymentToken() == null || request.getPaymentToken().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentToken es obligatorio");
            }
            Charge charge;
            try {
                Map<String, Object> chargeParams = new HashMap<>();
                chargeParams.put("amount", toStripeCents(expected));
                chargeParams.put("currency", "usd");
                chargeParams.put("source", request.getPaymentToken());
                chargeParams.put("description", "SkillNet - Plan: " + offering.getTitle());
                charge = Charge.create(chargeParams);
            } catch (StripeException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe rechazó el pago: " + e.getMessage());
            }

            if (!"succeeded".equals(charge.getStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La transacción fue rechazada.");
            }

            payment = buildServicePayment(user, offering, expected, "SUCCEEDED", "stripe_real", charge.getId(), now);
            try {
                payment.setGatewayResponse(objectMapper.readTree(charge.toJson()));
            } catch (Exception e) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo serializar la respuesta de Stripe.");
            }
            paymentRepository.save(payment);
            entitlementService.grantFromPayment(payment);
            notifyPlanPurchase(user, offering);
            message = "Plan activado. ID Stripe: " + charge.getId();
        }

        auditService.logAction(
                AuditAction.PURCHASE_PLAN,
                AuditAction.ENTITY_SERVICE_OFFERING,
                offering.getId(),
                user.getEmail(),
                "Plan adquirido: \"" + offering.getTitle() + "\" por USD " + expected
                        + " (pago #" + payment.getId() + ")");

        return CheckoutPaymentResponseDTO.builder()
                .message(message)
                .status("success")
                .paymentId(payment.getId())
                .build();
    }

    private Payment buildServicePayment(
            User user,
            InfoproductorServiceOffering offering,
            BigDecimal amount,
            String status,
            String paymentMethod,
            String stripeCheckoutId,
            Instant now) {
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setCourse(null);
        payment.setServiceOffering(offering);
        payment.setAmount(amount);
        payment.setStatus(status);
        payment.setPaymentMethod(paymentMethod);
        payment.setStripeCheckoutId(stripeCheckoutId);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        payment.setClientName(buildFullName(user));
        payment.setClientEmail(user.getEmail());
        payment.setClientPhone(user.getPhone());
        payment.setDocumentNumber(user.getDocumentNumber());
        payment.setClientAddress(user.getAddress());
        payment.setDocumentSent(false);
        payment.setAccountingNotified(false);
        applyDefaultCompanyBilling(payment);
        return payment;
    }

    private void applyDefaultCompanyBilling(Payment payment) {
        payment.setCompanyName("InterCert Latam");
        payment.setCompanyRut("76.123.456-7");
        payment.setCompanyAddress("Av. Principal 123, Santiago, Chile");
        payment.setCompanyPhone("+56 2 2345 6789");
        payment.setCompanyEmail("info@intercertlatam.net");
    }

    private void notifyPlanPurchase(User user, InfoproductorServiceOffering offering) {
        notificationPublisher.publish(
                user,
                "purchase",
                "Plan activado",
                "Tu plan \"" + offering.getTitle() + "\" está activo. Ya puedes usar las herramientas de IA incluidas.",
                frontendBaseUrl + "/infoproductor/plans");
    }

    private String buildFullName(User user) {
        String fullName = String.join(
                        " ",
                        user.getFirstName() != null ? user.getFirstName().trim() : "",
                        user.getLastName() != null ? user.getLastName().trim() : "")
                .trim();
        return fullName.isBlank() ? user.getUsername() : fullName;
    }

    private boolean isStripeConfigured() {
        return stripeSecretKey != null && !stripeSecretKey.isBlank();
    }

    private int toStripeCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }
}
