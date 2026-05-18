package com.skillnet.persistence.entity.core;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "core_promotionquoteitem")
public class PromotionQuoteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private PromotionQuote quote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "base_amount", precision = 9, scale = 2, nullable = false)
    private BigDecimal baseAmount;

    @Column(name = "total_amount", precision = 9, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "discounts_json", columnDefinition = "jsonb", nullable = false)
    private JsonNode discountsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applied_rule_ids", columnDefinition = "jsonb", nullable = false)
    private JsonNode appliedRuleIds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
