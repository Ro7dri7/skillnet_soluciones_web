package com.skillnet.web.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ServiceOfferingResponseDTO {

    private Long id;
    private String section;
    private String title;
    private String description;
    private BigDecimal priceUsd;
    private String iconClass;
    private int sortOrder;
    private boolean active;
    private JsonNode features;
    private BigDecimal originalPriceUsd;
    private BigDecimal saveAmountUsd;
    private boolean featured;
    private String capabilityKey;
    private int includedUses;
    private Instant createdAt;
    private Instant updatedAt;
}
