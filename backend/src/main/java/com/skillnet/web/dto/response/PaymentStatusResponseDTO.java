package com.skillnet.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    private BigDecimal amount;
    private String currency;
    private Instant createdAt;
    private String clientName;
    private String clientEmail;
    private String paymentMethod;
    @Builder.Default
    private List<PaymentReceiptItemDTO> items = new ArrayList<>();
}
