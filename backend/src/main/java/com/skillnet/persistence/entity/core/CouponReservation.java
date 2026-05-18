package com.skillnet.persistence.entity.core;

import com.skillnet.persistence.entity.payments.Payment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Incluye restricción parcial única en Postgres ({@code uniq_reserved_coupon_once}) no reproducida aquí;
 * debe mantenerse vía migración Flyway/Liquibase o DDL nativo.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "core_couponreservation",
        indexes = @Index(columnList = "coupon_id,status,expires_at"))
public class CouponReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "status", length = 12, nullable = false)
    private String status = "reserved";

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "released_email_sent_at")
    private Instant releasedEmailSentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
