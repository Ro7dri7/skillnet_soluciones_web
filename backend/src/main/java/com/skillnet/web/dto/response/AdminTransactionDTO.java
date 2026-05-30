package com.skillnet.web.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminTransactionDTO {

    private Long id;
    private String buyerName;
    private String buyerInitials;
    private String courseTitle;
    private BigDecimal amount;
    private String status;
}
