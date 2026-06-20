package com.skillnet.web.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentStatusResponseDTO {

    private Long id;
    private String status;
    private Long courseId;
    private String courseTitle;
}
