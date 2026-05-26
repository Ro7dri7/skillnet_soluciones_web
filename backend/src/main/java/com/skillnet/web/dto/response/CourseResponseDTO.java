package com.skillnet.web.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseResponseDTO {

    private Long id;
    private String title;
    private String description;
    private String whatYouWillLearn;
    private String targetAudience;
    private int durationHours;
    private int durationMinutes;
    private String requirements;
    private String welcomeMessage;
    private String congratulationsMessage;
    private String category;
    private String subcategory;
    private String level;
    private String language;
    private String courseFormat;
    private JsonNode software;
    private boolean hasSubtitles;
    private boolean flexibleSchedule;
    private boolean hasPracticalExperience;
    private BigDecimal originalPrice;
    private BigDecimal price;
    private String currency;
    private boolean onSale;
    private boolean taxIncluded;
    private Long professorId;
    private ProfessorSummaryDTO professor;
    private String status;
    private Instant createdAt;
    private String slug;
    private String imageUrl;
    private String imageFile;
    private String videoUrl;
    private String videoFile;
    private BigDecimal affiliateCommission;
    private String affiliatePolicy;
    private String ally;
    private String securityStatus;
    private JsonNode securityScanReport;
    private Instant lastScannedAt;
}
