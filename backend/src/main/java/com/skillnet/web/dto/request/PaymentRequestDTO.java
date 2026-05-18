package com.skillnet.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentRequestDTO {

    @NotNull(message = "userId is required")
    private Long userId;

    private Long courseId;
    private Long serviceOfferingId;
    private Long couponId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "paymentMethod is required")
    @Size(max = 12)
    private String paymentMethod;

    @NotBlank(message = "status is required")
    @Size(max = 20)
    private String status;

    @NotBlank(message = "documentType is required")
    @Size(max = 10)
    private String documentType;

    @Size(max = 20)
    private String documentNumber;

    @Size(max = 255)
    private String clientName;

    @Email(message = "clientEmail must be valid")
    private String clientEmail;

    @Size(max = 20)
    private String clientRut;

    private String clientAddress;

    @Size(max = 20)
    private String clientPhone;

    @NotBlank(message = "companyName is required")
    @Size(max = 255)
    private String companyName;

    @NotBlank(message = "companyRut is required")
    @Size(max = 12)
    private String companyRut;

    @NotBlank(message = "companyAddress is required")
    private String companyAddress;

    @NotBlank(message = "companyPhone is required")
    @Size(max = 20)
    private String companyPhone;

    @NotBlank(message = "companyEmail is required")
    @Email(message = "companyEmail must be valid")
    private String companyEmail;

    private boolean documentSent;
    private boolean accountingNotified;
    private Instant createdAt;
    private Instant updatedAt;
}
