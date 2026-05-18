package com.skillnet.persistence.entity.payments;

import com.fasterxml.jackson.databind.JsonNode;
import com.skillnet.persistence.entity.core.User;
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
import jakarta.persistence.UniqueConstraint;
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
@Table(
        name = "payments_idempotencykey",
        uniqueConstraints = @UniqueConstraint(name = "uniq_idem_user_endpoint_key", columnNames = {"user_id", "endpoint", "key"}),
        indexes = {
            @Index(name = "payments_idempotencykey_endpoint_key_idx", columnList = "endpoint,key"),
            @Index(name = "payments_idempotencykey_expires_at_idx", columnList = "expires_at")
        })
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "endpoint", nullable = false, length = 80)
    private String endpoint;

    @Column(name = "key", nullable = false, length = 120)
    private String key;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_json", columnDefinition = "jsonb", nullable = false)
    private JsonNode responseJson;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
