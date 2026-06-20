package com.skillnet.web.dto.response;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServiceEntitlementResponseDTO {
    private final Long id;
    private final String status;
    private final int usesRemaining;
    private final String capabilityKey;
    private final String offeringTitle;
    private final Instant createdAt;
}
