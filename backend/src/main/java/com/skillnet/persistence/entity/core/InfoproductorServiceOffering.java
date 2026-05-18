package com.skillnet.persistence.entity.core;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "core_infoproductorserviceoffering")
public class InfoproductorServiceOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section", length = 20, nullable = false)
    private String section;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "price_usd", precision = 10, scale = 2, nullable = false)
    private BigDecimal priceUsd;

    @Column(name = "icon_class", length = 120)
    private String iconClass;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb", nullable = false)
    private JsonNode features;

    @Column(name = "original_price_usd", precision = 10, scale = 2)
    private BigDecimal originalPriceUsd;

    @Column(name = "save_amount_usd", precision = 10, scale = 2)
    private BigDecimal saveAmountUsd;

    @Column(name = "is_featured", nullable = false)
    private boolean featured;

    @Column(name = "capability_key", length = 64)
    private String capabilityKey = "";

    @Column(name = "included_uses", nullable = false)
    private int includedUses = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
