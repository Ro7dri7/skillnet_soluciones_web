package com.skillnet.persistence.entity.core;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "core_auditlog",
        indexes = {
            @Index(columnList = "user_id,timestamp"),
            @Index(columnList = "model_name,timestamp"),
            @Index(columnList = "action,timestamp")
        })
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "action", length = 20, nullable = false)
    private String action;

    @Column(name = "model_name", length = 100, nullable = false)
    private String modelName;

    @Column(name = "object_id")
    private Integer objectId;

    @Column(name = "object_repr", length = 200, nullable = false)
    private String objectRepr;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes", columnDefinition = "jsonb")
    private JsonNode changes;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "country_code", length = 5)
    private String countryCode;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
}
