package com.skillnet.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentResponseDTO {

    private Long id;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private String gatewayReference;
    private Long userId;
    private UserSummaryDTO user;
    private Long courseId;
    private CourseSummaryDTO course;
    private Long serviceOfferingId;
    private Long couponId;
    private CouponSummaryDTO coupon;
    private String documentType;
    private String documentNumber;
    private String clientName;
    private String clientEmail;
    private String clientRut;
    private String clientAddress;
    private String clientPhone;
    private String companyName;
    private String companyRut;
    private String companyAddress;
    private String companyPhone;
    private String companyEmail;
    private boolean documentSent;
    private boolean accountingNotified;
    private Instant createdAt;
    private Instant updatedAt;
}
