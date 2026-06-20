package com.skillnet.web.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ServiceOfferingRequestDTO {

    @NotBlank
    @Size(max = 20)
    private String section;

    @NotBlank
    @Size(max = 200)
    private String title;

    private String description;

    @NotNull
    private BigDecimal priceUsd;

    @Size(max = 120)
    private String iconClass;

    private int sortOrder;
    private boolean active = true;
    private JsonNode features;
    private BigDecimal originalPriceUsd;
    private BigDecimal saveAmountUsd;
    private boolean featured;
    private String capabilityKey;
    private int includedUses = 1;
}
