package com.skillnet.api.dto.payments;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutQuoteResponseDTO {
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private String couponCode;
    private Integer couponPercentOff;
    private BigDecimal couponAmountOff;
    private String couponLabel;
    private boolean couponValid;
    private String message;
    private List<CheckoutQuoteLineDTO> lines;
}
