package com.skillnet.api.dto.payments;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutQuoteLineDTO {
    private Long courseId;
    private String title;
    private BigDecimal baseAmount;
    private BigDecimal totalAmount;
}
