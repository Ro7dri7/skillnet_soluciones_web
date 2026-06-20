package com.skillnet.api.dto.payments;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class CheckoutRequestDTO {
    private Long courseId;
    private List<Long> courseIds;
    private BigDecimal amount;
    private String paymentToken;
    private String couponCode;
}
