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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "core_lesson")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "resource_url", length = 1000)
    private String resourceUrl;

    @Column(name = "resource_file", length = 500)
    private String resourceFile;

    @Column(name = "content_type", length = 20, nullable = false)
    private String contentType = "lesson";

    @Column(name = "\"order\"", nullable = false)
    private int orderIndex;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "draft";

    @Column(name = "version", nullable = false)
    private int version = 1;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "security_status", length = 20, nullable = false)
    private String securityStatus = "pending";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "security_scan_report", columnDefinition = "jsonb", nullable = false)
    private JsonNode securityScanReport;

    @Column(name = "last_scanned_at")
    private Instant lastScannedAt;
}
