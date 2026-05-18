package com.skillnet.web.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LessonRequestDTO {

    @NotNull(message = "courseId is required")
    private Long courseId;

    private Long sectionId;

    @NotBlank(message = "title is required")
    @Size(max = 255)
    private String title;

    private String content;

    @Size(max = 1000)
    private String resourceUrl;

    @Size(max = 500)
    private String resourceFile;

    @NotBlank(message = "contentType is required")
    @Size(max = 20)
    private String contentType;

    @Min(value = 0, message = "orderIndex must be zero or positive")
    private int orderIndex;

    @NotBlank(message = "status is required")
    @Size(max = 20)
    private String status;

    @Min(value = 1, message = "version must be at least 1")
    private int version;

    private Instant updatedAt;

    @NotBlank(message = "securityStatus is required")
    @Size(max = 20)
    private String securityStatus;

    private JsonNode securityScanReport;
    private Instant lastScannedAt;
}
