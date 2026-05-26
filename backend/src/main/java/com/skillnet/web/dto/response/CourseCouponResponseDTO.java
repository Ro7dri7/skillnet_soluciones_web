package com.skillnet.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseCouponResponseDTO {

    private Long id;
    private String code;
    private int percentOff;
    private BigDecimal amountOff;
    private boolean active;
    private Instant validTo;
}
