package com.skillnet.web.dto.response.analytics;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecentTransactionDTO {

    private String date;
    private String courseName;
    private String studentName;
    private BigDecimal amount;
}
