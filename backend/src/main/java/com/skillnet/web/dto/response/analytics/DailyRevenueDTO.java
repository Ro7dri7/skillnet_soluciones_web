package com.skillnet.web.dto.response.analytics;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DailyRevenueDTO {

    private String date;
    private BigDecimal amount;
}
