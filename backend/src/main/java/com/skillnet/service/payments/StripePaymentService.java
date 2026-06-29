package com.skillnet.service.payments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillnet.api.dto.payments.CheckoutPaymentResponseDTO;
import com.skillnet.api.dto.payments.CheckoutQuoteResponseDTO;
import com.skillnet.api.dto.payments.CheckoutRequestDTO;
import com.skillnet.domain.AuditAction;
import com.skillnet.persistence.entity.core.Coupon;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.Enrollment;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.entity.payments.Payment;
import com.skillnet.persistence.repository.CouponRepository;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.persistence.repository.PaymentRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.service.AuditService;
import com.skillnet.service.notification.NotificationPublisher;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final CouponRepository couponRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final CheckoutQuoteService checkoutQuoteService;
    private final NotificationPublisher notificationPublisher;
    private final com.skillnet.service.mail.EmailService emailService;

    @Value("${stripe.api.secretKey}")
    private String stripeSecretKey;

    @Value("${skillnet.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Transactional
    public CheckoutPaymentResponseDTO processRealPayment(CheckoutRequestDTO request, Long userId) {
        validateRequest(request);

        List<Long> courseIds = resolveCourseIds(request);
        assertNotAlreadyEnrolled(userId, courseIds);

        User currentUser = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        List<Course> courses = courseIds.stream()
                .map(id -> courseRepository
                        .findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado")))
                .toList();
        Course primaryCourse = courses.get(0);

        CheckoutQuoteResponseDTO quote =
                checkoutQuoteService.quote(courseIds, request.getCouponCode());
        assertAmountMatchesQuote(request.getAmount(), quote.getTotal());

        Optional<Coupon> couponOpt = checkoutQuoteService.resolveCoupon(request.getCouponCode(), courses);
        Coupon appliedCoupon = couponOpt.orElse(null);

        if (quote.getTotal().signum() == 0) {
            Payment payment = processFreePayment(currentUser, courseIds, courses, primaryCourse, quote, appliedCoupon);
            return CheckoutPaymentResponseDTO.builder()
                    .message("Inscripción gratuita confirmada con cupón.")
                    .status("success")
                    .paymentId(payment.getId())
                    .build();
        }

        if (request.getPaymentToken() == null || request.getPaymentToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentToken es obligatorio");
        }

        Payment payment;
        String message;

        if (!isStripeConfigured() || isDevMockToken(request.getPaymentToken())) {
            if (isStripeConfigured() && isDevMockToken(request.getPaymentToken())) {
                log.warn(
                        "Token dev_mock: procesando pago simulado aunque STRIPE_SECRET_KEY esté configurada");
            } else {
                log.warn(
                        "STRIPE_SECRET_KEY no configurada: procesando pago simulado en local para curso {}",
                        primaryCourse.getId());
            }
            payment = processDevMockPayment(currentUser, courseIds, courses, primaryCourse, quote, appliedCoupon);
            message = "Pago simulado (desarrollo local). ID: " + payment.getStripeCheckoutId();
        } else {
            Charge charge;
            try {
                Map<String, Object> chargeParams = new HashMap<>();
                chargeParams.put("amount", toStripeCents(quote.getTotal()));
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
            payment = buildPayment(
                    currentUser,
                    primaryCourse,
                    quote.getTotal(),
                    "SUCCEEDED",
                    "stripe_real",
                    charge.getId(),
                    now,
                    appliedCoupon);

            try {
                JsonNode realGatewayResponse = objectMapper.readTree(charge.toJson());
                payment.setGatewayResponse(realGatewayResponse);
            } catch (Exception e) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo serializar la respuesta de Stripe.");
            }

            paymentRepository.save(payment);
            redeemCoupon(appliedCoupon);
            createEnrollments(currentUser, courseIds, now);
            logPurchase(currentUser, primaryCourse, payment.getId(), quote.getTotal(), courseIds);
            notifyPurchase(currentUser, courses, payment);
            message = "Transacción aprobada de forma segura por Stripe. ID: " + charge.getId();
        }

        return CheckoutPaymentResponseDTO.builder()
                .message(message)
                .status("success")
                .paymentId(payment.getId())
                .build();
    }

    private Payment processFreePayment(
            User currentUser,
            List<Long> courseIds,
            List<Course> courses,
            Course primaryCourse,
            CheckoutQuoteResponseDTO quote,
            Coupon appliedCoupon) {
        Instant now = Instant.now();
        Payment payment = buildPayment(
                currentUser,
                primaryCourse,
                quote.getTotal(),
                "SUCCEEDED",
                "coupon_free",
                "free_" + java.util.UUID.randomUUID(),
                now,
                appliedCoupon);
        payment.setGatewayResponse(objectMapper.createObjectNode().put("mode", "free_coupon"));
        paymentRepository.save(payment);
        redeemCoupon(appliedCoupon);
        createEnrollments(currentUser, courseIds, now);
        logPurchase(currentUser, primaryCourse, payment.getId(), quote.getTotal(), courseIds);
        notifyPurchase(currentUser, courses, payment);
        return payment;
    }

    private Payment processDevMockPayment(
            User currentUser,
            List<Long> courseIds,
            List<Course> courses,
            Course primaryCourse,
            CheckoutQuoteResponseDTO quote,
            Coupon appliedCoupon) {
        Instant now = Instant.now();
        String mockChargeId = "dev_" + java.util.UUID.randomUUID();

        Payment payment = buildPayment(
                currentUser,
                primaryCourse,
                quote.getTotal(),
                "SUCCEEDED",
                "stripe_dev",
                mockChargeId,
                now,
                appliedCoupon);
        payment.setGatewayResponse(objectMapper.createObjectNode().put("mode", "dev_mock"));

        paymentRepository.save(payment);
        redeemCoupon(appliedCoupon);
        createEnrollments(currentUser, courseIds, now);
        logPurchase(currentUser, primaryCourse, payment.getId(), quote.getTotal(), courseIds);
        notifyPurchase(currentUser, courses, payment);

        return payment;
    }

    private void redeemCoupon(Coupon coupon) {
        if (coupon == null) {
            return;
        }
        coupon.setTimesRedeemed(coupon.getTimesRedeemed() + 1);
        couponRepository.save(coupon);
    }

    private void notifyPurchase(User buyer, Iterable<Course> courses, Payment payment) {
        notificationPublisher.publish(
                buyer,
                "purchase",
                "Compra confirmada",
                "Tu pago fue procesado correctamente. Ya puedes acceder a tus cursos.",
                frontendBaseUrl + "/mis-cursos");

        List<Course> courseList = new java.util.ArrayList<>();
        for (Course course : courses) {
            courseList.add(course);
        }
        emailService.sendPurchaseReceiptEmail(payment, courseList, frontendBaseUrl + "/mis-cursos");

        Set<Long> notifiedProfessors = new HashSet<>();
        for (Course course : courses) {
            User professor = course.getProfessor();
            if (professor == null || notifiedProfessors.contains(professor.getId())) {
                continue;
            }
            notifiedProfessors.add(professor.getId());
            notificationPublisher.publish(
                    professor,
                    "sale",
                    "Nueva venta",
                    buyer.getEmail() + " compró \"" + course.getTitle() + "\".",
                    frontendBaseUrl + "/dashboard/infoproductor");
        }
    }

    private void assertAmountMatchesQuote(BigDecimal requestedAmount, BigDecimal expectedTotal) {
        if (requestedAmount == null || expectedTotal == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Monto inválido");
        }
        BigDecimal requested = requestedAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal expected = expectedTotal.setScale(2, RoundingMode.HALF_UP);
        if (requested.compareTo(expected) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El monto enviado no coincide con la cotización. Recalcula el total e intenta de nuevo.");
        }
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
            Instant now,
            Coupon coupon) {
        Payment payment = new Payment();
        payment.setUser(currentUser);
        payment.setCourse(primaryCourse);
        payment.setAmount(amount);
        payment.setStatus(status);
        payment.setPaymentMethod(paymentMethod);
        payment.setStripeCheckoutId(stripeCheckoutId);
        payment.setCoupon(coupon);
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

    private boolean isDevMockToken(String token) {
        return "dev_mock_checkout".equals(token) || "dev_mock_yape".equals(token);
    }

    private void validateRequest(CheckoutRequestDTO request) {
        if (request.getCourseId() == null && (request.getCourseIds() == null || request.getCourseIds().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "courseId es obligatorio");
        }
        if (request.getAmount() == null || request.getAmount().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount inválido");
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

    private void logPurchase(
            User buyer, Course primaryCourse, Long paymentId, BigDecimal amount, List<Long> courseIds) {
        auditService.logAction(
                AuditAction.PURCHASE_COURSE,
                AuditAction.ENTITY_PAYMENT,
                paymentId,
                buyer.getEmail(),
                "Compra de producto(s): "
                        + primaryCourse.getTitle()
                        + " — USD "
                        + amount
                        + (courseIds.size() > 1 ? " (+" + (courseIds.size() - 1) + " productos)" : "")
                        + " — pago #"
                        + paymentId);
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
