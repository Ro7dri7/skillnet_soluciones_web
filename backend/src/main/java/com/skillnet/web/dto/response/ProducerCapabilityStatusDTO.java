package com.skillnet.web.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProducerCapabilityStatusDTO {
    private final String capabilityKey;
    private final boolean active;
    private final int usesRemaining;
}
