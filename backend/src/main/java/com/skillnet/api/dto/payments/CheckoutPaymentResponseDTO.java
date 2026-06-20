package com.skillnet.api.dto.payments;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckoutPaymentResponseDTO {
    private String message;
    private String status;
    private Long paymentId;
}
