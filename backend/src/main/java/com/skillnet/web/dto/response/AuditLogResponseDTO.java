package com.skillnet.web.dto.response;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditLogResponseDTO {

    Long id;
    String action;
    String entityName;
    Long entityId;
    String userEmail;
    String userDisplayName;
    String ipAddress;
    String userAgent;
    String details;
    Instant timestamp;
}
