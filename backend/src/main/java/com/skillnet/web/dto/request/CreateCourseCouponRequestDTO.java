package com.skillnet.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateCourseCouponRequestDTO {

    @NotBlank
    @Size(max = 50)
    private String code;

    private int percentOff;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal amountOff;
}
