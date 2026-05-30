package com.skillnet.web.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminTopProducerDTO {

    private Long id;
    private String name;
    private String category;
    private BigDecimal revenue;
}
