package com.skillnet.web.dto.response;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CoursePricingResponseDTO {

    private Long id;
    private String currency;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private boolean onSale;
    private BigDecimal discountPrice;
    private BigDecimal affiliateCommission;
    private String affiliationType;
}
