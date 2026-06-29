package com.skillnet.web.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentReceiptItemDTO {

    private Long courseId;
    private String courseTitle;
    private String courseFormat;
}
