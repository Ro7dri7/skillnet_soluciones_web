package com.skillnet.persistence.entity.core;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "core_course")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "what_you_will_learn", columnDefinition = "text")
    private String whatYouWillLearn;

    @Column(name = "target_audience", columnDefinition = "text")
    private String targetAudience;

    @Column(name = "duration_hours", nullable = false)
    private int durationHours;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "requirements", columnDefinition = "text")
    private String requirements;

    @Column(name = "welcome_message", columnDefinition = "text")
    private String welcomeMessage;

    @Column(name = "congratulations_message", columnDefinition = "text")
    private String congratulationsMessage;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "subcategory", length = 200)
    private String subcategory;

    @Column(name = "level", length = 20, nullable = false)
    private String level = "beginner";

    @Column(name = "language", length = 10, nullable = false)
    private String language = "es";

    /** Formato del infoproducto: course, ebook, audiobook, podcast, etc. */
    @Column(name = "course_format", length = 30, nullable = false)
    private String courseFormat = "course";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "software", columnDefinition = "jsonb", nullable = false)
    private JsonNode software;

    @Column(name = "has_subtitles", nullable = false)
    private boolean hasSubtitles;

    @Column(name = "is_flexible_schedule", nullable = false)
    private boolean flexibleSchedule;

    @Column(name = "has_practical_experience", nullable = false)
    private boolean hasPracticalExperience;

    @Column(name = "original_price", precision = 9, scale = 2, nullable = false)
    private BigDecimal originalPrice;

    @Column(name = "price", precision = 9, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "USD";

    @Column(name = "is_on_sale", nullable = false)
    private boolean onSale;

    @Column(name = "tax_included", nullable = false)
    private boolean taxIncluded;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    private User professor;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "draft"; // DRAFT por defecto (valor persistido en minúsculas)

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "slug", length = 300, unique = true)
    private String slug;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "image_file", length = 500)
    private String imageFile;

    @Column(name = "video_url", length = 1000)
    private String videoUrl;

    @Column(name = "video_file", length = 500)
    private String videoFile;

    @Column(name = "affiliate_commission", precision = 5, scale = 2, nullable = false)
    private BigDecimal affiliateCommission;

    @Column(name = "affiliate_policy", length = 20, nullable = false)
    private String affiliatePolicy = "all";

    @Column(name = "ally", length = 100, nullable = false)
    private String ally = "intercert";

    @Column(name = "security_status", length = 20, nullable = false)
    private String securityStatus = "pending";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "security_scan_report", columnDefinition = "jsonb")
    private JsonNode securityScanReport;

    @Column(name = "last_scanned_at")
    private Instant lastScannedAt;

    @ManyToMany(mappedBy = "featuredCourses", fetch = FetchType.LAZY)
    private Set<User> usersWhoFeatured = new HashSet<>();

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private Set<Section> sections = new HashSet<>();

    public String getFormat() {
        return courseFormat;
    }

    public void setFormat(String format) {
        this.courseFormat = format;
    }

    public String getAudience() {
        return targetAudience;
    }

    public void setAudience(String audience) {
        this.targetAudience = audience;
    }
}
