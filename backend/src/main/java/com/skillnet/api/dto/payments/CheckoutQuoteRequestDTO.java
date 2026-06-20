package com.skillnet.api.dto.payments;

import java.util.List;
import lombok.Data;

@Data
public class CheckoutQuoteRequestDTO {
    private List<Long> courseIds;
    private String couponCode;
}
