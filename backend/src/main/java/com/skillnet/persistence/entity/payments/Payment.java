package com.skillnet.persistence.entity.payments;

import com.fasterxml.jackson.databind.JsonNode;
import com.skillnet.persistence.entity.core.Coupon;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.InfoproductorServiceOffering;
import com.skillnet.persistence.entity.core.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Mapea {@code payments_payment} (Django app {@code payments}).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments_payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_offering_id")
    private InfoproductorServiceOffering serviceOffering;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_method", length = 12, nullable = false)
    private String paymentMethod = "dlocal";

    @Column(name = "paypal_order_id", unique = true, length = 255)
    private String paypalOrderId;

    @Column(name = "paypal_payment_id", length = 255)
    private String paypalPaymentId;

    @Column(name = "stripe_checkout_id", unique = true, length = 255)
    private String stripeCheckoutId;

    @Column(name = "mercadopago_preference_id", length = 255)
    private String mercadopagoPreferenceId;

    @Column(name = "mercadopago_payment_id", unique = true, length = 255)
    private String mercadopagoPaymentId;

    @Column(name = "dlocal_checkout_id", unique = true, length = 255)
    private String dlocalCheckoutId;

    @Column(name = "dlocal_payment_id", unique = true, length = 255)
    private String dlocalPaymentId;

    @Column(name = "izipay_transaction_id", unique = true, length = 50)
    private String izipayTransactionId;

    @Column(name = "izipay_order_number", length = 50)
    private String izipayOrderNumber;

    @Column(name = "izipay_payment_link_id", length = 100)
    private String izipayPaymentLinkId;

    @Column(name = "izipay_response_code", length = 10)
    private String izipayResponseCode;

    @Column(name = "izipay_signature", columnDefinition = "text")
    private String izipaySignature;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "izipay_payload", columnDefinition = "jsonb")
    private JsonNode izipayPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_response", columnDefinition = "jsonb")
    private JsonNode gatewayResponse;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    @Column(name = "document_type", length = 10, nullable = false)
    private String documentType = "boleta";

    @Column(name = "document_number", length = 20)
    private String documentNumber;

    @Column(name = "client_name", length = 255)
    private String clientName;

    @Column(name = "client_email")
    private String clientEmail;

    @Column(name = "client_rut", length = 20)
    private String clientRut;

    @Column(name = "client_address", columnDefinition = "text")
    private String clientAddress;

    @Column(name = "client_phone", length = 20)
    private String clientPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "company_rut", length = 12)
    private String companyRut;

    @Column(name = "company_address", columnDefinition = "text")
    private String companyAddress;

    @Column(name = "company_phone", length = 20)
    private String companyPhone;

    @Column(name = "company_email")
    private String companyEmail;

    @Column(name = "document_sent", nullable = false)
    private boolean documentSent;

    @Column(name = "accounting_notified", nullable = false)
    private boolean accountingNotified;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "payment")
    private List<PaymentItem> items = new ArrayList<>();
}
