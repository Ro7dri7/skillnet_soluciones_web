package com.skillnet.web.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponSummaryDTO {

    private Long id;
    private String code;
    private int percentOff;
    private BigDecimal amountOff;
    private boolean active;
}
