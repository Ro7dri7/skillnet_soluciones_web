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
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "core_assetuploadsession")
public class AssetUploadSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "token", nullable = false, unique = true)
    private UUID token;

    @Column(name = "storage_kind", length = 20, nullable = false)
    private String storageKind = "resource";

    @Column(name = "original_filename", length = 255, nullable = false)
    private String originalFilename = "";

    @Column(name = "storage_key", length = 500, nullable = false)
    private String storageKey = "";

    @Column(name = "public_url", length = 1000, nullable = false)
    private String publicUrl = "";

    @Column(name = "status", length = 20, nullable = false)
    private String status = "pending";

    @Column(name = "error_message", columnDefinition = "text", nullable = false)
    private String errorMessage = "";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    private JsonNode metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
