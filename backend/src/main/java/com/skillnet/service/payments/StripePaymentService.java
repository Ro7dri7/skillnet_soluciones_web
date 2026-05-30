package com.skillnet.service.payments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillnet.api.dto.payments.CheckoutRequestDTO;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.Enrollment;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.entity.payments.Payment;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.persistence.repository.PaymentRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
public class StripePaymentService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentService.class);

    private static final String DEFAULT_COMPANY_NAME = "InterCert Latam";
    private static final String DEFAULT_COMPANY_RUT = "76.123.456-7";
    private static final String DEFAULT_COMPANY_ADDRESS = "Av. Principal 123, Santiago, Chile";
    private static final String DEFAULT_COMPANY_PHONE = "+56 2 2345 6789";
    private static final String DEFAULT_COMPANY_EMAIL = "info@intercertlatam.net";

    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${stripe.api.secretKey}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Transactional
    public String processRealPayment(CheckoutRequestDTO request, Long userId) {
        validateRequest(request);

        List<Long> courseIds = resolveCourseIds(request);
        assertNotAlreadyEnrolled(userId, courseIds);

        User currentUser = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Course primaryCourse = courseRepository
                .findById(courseIds.get(0))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        if (!isStripeConfigured()) {
            log.warn(
                    "STRIPE_SECRET_KEY no configurada: procesando pago simulado en local para curso {}",
                    primaryCourse.getId());
            return processDevMockPayment(request, userId, currentUser, courseIds, primaryCourse);
        }

        Charge charge;
        try {
            Map<String, Object> chargeParams = new HashMap<>();
            chargeParams.put("amount", toStripeCents(request.getAmount()));
            chargeParams.put("currency", "usd");
            chargeParams.put("source", request.getPaymentToken());
            chargeParams.put("description", "Skillnet - Compra de curso: " + primaryCourse.getTitle());
            charge = Charge.create(chargeParams);
        } catch (StripeException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Stripe rechazó el pago: " + e.getMessage());
        }

        if (!"succeeded".equals(charge.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La transacción fue rechazada por la entidad bancaria.");
        }

        Instant now = Instant.now();
        Payment payment = buildPayment(
                currentUser,
                primaryCourse,
                request.getAmount(),
                "SUCCEEDED",
                "stripe_real",
                charge.getId(),
                now);

        try {
            JsonNode realGatewayResponse = objectMapper.readTree(charge.toJson());
            payment.setGatewayResponse(realGatewayResponse);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo serializar la respuesta de Stripe.");
        }

        paymentRepository.save(payment);
        createEnrollments(currentUser, courseIds, now);

        return "Transacción aprobada de forma segura por Stripe. ID: " + charge.getId();
    }

    private String processDevMockPayment(
            CheckoutRequestDTO request,
            Long userId,
            User currentUser,
            List<Long> courseIds,
            Course primaryCourse) {
        Instant now = Instant.now();
        String mockChargeId = "dev_" + UUID.randomUUID();

        Payment payment = buildPayment(
                currentUser,
                primaryCourse,
                request.getAmount(),
                "SUCCEEDED",
                "stripe_dev",
                mockChargeId,
                now);
        payment.setGatewayResponse(objectMapper.createObjectNode().put("mode", "dev_mock"));

        paymentRepository.save(payment);
        createEnrollments(currentUser, courseIds, now);

        return "Pago simulado (desarrollo local). ID: " + mockChargeId;
    }

    private void assertNotAlreadyEnrolled(Long userId, List<Long> courseIds) {
        for (Long courseId : courseIds) {
            if (enrollmentRepository.existsByUser_IdAndCourse_Id(userId, courseId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "El estudiante ya se encuentra matriculado en este curso.");
            }
        }
    }

    private Payment buildPayment(
            User currentUser,
            Course primaryCourse,
            BigDecimal amount,
            String status,
            String paymentMethod,
            String stripeCheckoutId,
            Instant now) {
        Payment payment = new Payment();
        payment.setUser(currentUser);
        payment.setCourse(primaryCourse);
        payment.setAmount(amount);
        payment.setStatus(status);
        payment.setPaymentMethod(paymentMethod);
        payment.setStripeCheckoutId(stripeCheckoutId);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        applyClientBillingFromUser(payment, currentUser);
        applyDefaultCompanyBilling(payment);
        return payment;
    }

    private void applyDefaultCompanyBilling(Payment payment) {
        payment.setCompanyName(DEFAULT_COMPANY_NAME);
        payment.setCompanyRut(DEFAULT_COMPANY_RUT);
        payment.setCompanyAddress(DEFAULT_COMPANY_ADDRESS);
        payment.setCompanyPhone(DEFAULT_COMPANY_PHONE);
        payment.setCompanyEmail(DEFAULT_COMPANY_EMAIL);
        payment.setDocumentSent(false);
        payment.setAccountingNotified(false);
    }

    private void createEnrollments(User currentUser, List<Long> courseIds, Instant now) {
        for (Long courseId : courseIds) {
            Course course = courseRepository
                    .findById(courseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));
            Enrollment enrollment = new Enrollment();
            enrollment.setUser(currentUser);
            enrollment.setCourse(course);
            enrollment.setEnrollmentType("PAID");
            enrollment.setEnrolledAt(now);
            enrollmentRepository.save(enrollment);
        }
    }

    private boolean isStripeConfigured() {
        return stripeSecretKey != null && !stripeSecretKey.isBlank();
    }

    private void validateRequest(CheckoutRequestDTO request) {
        if (request.getCourseId() == null && (request.getCourseIds() == null || request.getCourseIds().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "courseId es obligatorio");
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount debe ser mayor a cero");
        }
        if (request.getPaymentToken() == null || request.getPaymentToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentToken es obligatorio");
        }
    }

    private List<Long> resolveCourseIds(CheckoutRequestDTO request) {
        if (request.getCourseIds() != null && !request.getCourseIds().isEmpty()) {
            return request.getCourseIds();
        }
        return List.of(request.getCourseId());
    }

    private int toStripeCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private void applyClientBillingFromUser(Payment payment, User user) {
        String fullName = String.join(
                        " ",
                        user.getFirstName() != null ? user.getFirstName().trim() : "",
                        user.getLastName() != null ? user.getLastName().trim() : "")
                .trim();
        if (fullName.isBlank()) {
            fullName = user.getUsername();
        }
        payment.setClientName(fullName);
        payment.setClientEmail(user.getEmail());
        payment.setClientPhone(user.getPhone());
        payment.setDocumentNumber(user.getDocumentNumber());
        payment.setClientAddress(user.getAddress());
    }
}
