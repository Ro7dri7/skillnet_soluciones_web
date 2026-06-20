package com.skillnet.web.dto.response;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NotificationResponseDTO {

    private Long id;
    private String notificationType;
    private String title;
    private String message;
    private boolean read;
    private Instant createdAt;
    private String link;
}
