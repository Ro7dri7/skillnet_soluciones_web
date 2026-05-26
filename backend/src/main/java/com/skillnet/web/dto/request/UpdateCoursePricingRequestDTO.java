package com.skillnet.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCoursePricingRequestDTO {

    @Size(max = 3)
    private String currency;

    @DecimalMin(value = "0.01", message = "price must be greater than zero")
    private BigDecimal price;

    private Boolean onSale;

    @DecimalMin(value = "0.01", message = "discount price must be greater than zero")
    private BigDecimal discountPrice;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal affiliateCommission;

    @Size(max = 20)
    private String affiliationType;
}
