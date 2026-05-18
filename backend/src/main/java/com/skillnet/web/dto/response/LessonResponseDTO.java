package com.skillnet.web.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LessonResponseDTO {

    private Long id;
    private Long courseId;
    private CourseSummaryDTO course;
    private Long sectionId;
    private SectionSummaryDTO section;
    private String title;
    private String content;
    private String resourceUrl;
    private String resourceFile;
    private String contentType;
    private int orderIndex;
    private String status;
    private int version;
    private Instant updatedAt;
    private String securityStatus;
    private JsonNode securityScanReport;
    private Instant lastScannedAt;
}
