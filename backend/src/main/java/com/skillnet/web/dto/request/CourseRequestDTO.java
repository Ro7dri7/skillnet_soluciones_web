package com.skillnet.web.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseRequestDTO {

    @NotBlank(message = "title is required")
    @Size(max = 255)
    private String title;

    private String description;
    private String whatYouWillLearn;
    private String targetAudience;

    @Min(value = 0, message = "durationHours must be zero or positive")
    private int durationHours;

    @Min(value = 0, message = "durationMinutes must be zero or positive")
    private int durationMinutes;

    private String requirements;

    @Size(max = 100)
    private String category;

    @Size(max = 200)
    private String subcategory;

    @NotBlank(message = "level is required")
    @Size(max = 20)
    private String level;

    @NotBlank(message = "language is required")
    @Size(max = 10)
    private String language;

    @NotBlank(message = "courseFormat is required")
    @Size(max = 30)
    private String courseFormat;

    private JsonNode software;
    private boolean hasSubtitles;
    private boolean flexibleSchedule;
    private boolean hasPracticalExperience;

    @NotNull(message = "originalPrice is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "originalPrice must be greater than zero")
    private BigDecimal originalPrice;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "price must be greater than zero")
    private BigDecimal price;

    @NotBlank(message = "currency is required")
    @Size(max = 3)
    private String currency;

    private boolean onSale;
    private boolean taxIncluded;
    private Long professorId;

    @NotBlank(message = "status is required")
    @Size(max = 20)
    private String status;

    private Instant createdAt;

    @Size(max = 300)
    private String slug;

    @Size(max = 200)
    private String imageUrl;

    @Size(max = 500)
    private String imageFile;

    @Size(max = 200)
    private String videoUrl;

    @Size(max = 500)
    private String videoFile;

    @NotNull(message = "affiliateCommission is required")
    @DecimalMin(value = "0.0", message = "affiliateCommission must be zero or positive")
    private BigDecimal affiliateCommission;

    @NotBlank(message = "affiliatePolicy is required")
    @Size(max = 20)
    private String affiliatePolicy;

    @NotBlank(message = "ally is required")
    @Size(max = 100)
    private String ally;

    @NotBlank(message = "securityStatus is required")
    @Size(max = 20)
    private String securityStatus;

    private JsonNode securityScanReport;
    private Instant lastScannedAt;
}
