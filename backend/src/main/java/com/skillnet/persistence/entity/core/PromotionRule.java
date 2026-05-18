package com.skillnet.persistence.entity.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "core_promotionrule")
public class PromotionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "code", length = 50, unique = true)
    private String code;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "discount_type", length = 10, nullable = false)
    private String discountType = "percent";

    @Column(name = "percent_off", nullable = false)
    private int percentOff;

    @Column(name = "amount_off", precision = 9, scale = 2, nullable = false)
    private BigDecimal amountOff;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "stackable", nullable = false)
    private boolean stackable = true;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "max_redemptions_total")
    private Integer maxRedemptionsTotal;

    @Column(name = "times_redeemed", nullable = false)
    private int timesRedeemed;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "core_promotionrule_courses",
            joinColumns = @JoinColumn(name = "promotionrule_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id"))
    private Set<Course> courses = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
