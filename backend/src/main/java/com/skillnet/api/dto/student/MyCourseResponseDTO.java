package com.skillnet.api.dto.student;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MyCourseResponseDTO {
    private Long courseId;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private String authorName;
    private Integer progressPercentage;
    private Instant enrolledAt;
}
