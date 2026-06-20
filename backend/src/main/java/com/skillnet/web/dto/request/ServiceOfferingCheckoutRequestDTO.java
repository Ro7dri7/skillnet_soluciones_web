package com.skillnet.web.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ServiceOfferingCheckoutRequestDTO {
    @NotNull
    private Long serviceOfferingId;

    @NotNull
    private BigDecimal amount;

    private String paymentToken;
}
