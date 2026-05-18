package com.skillnet.web.dto.response;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EnrollmentResponseDTO {

    private Long id;
    private Long userId;
    private UserSummaryDTO user;
    private Long courseId;
    private CourseSummaryDTO course;
    private Instant enrolledAt;
    private String enrollmentType;
    private Long enrolledById;
    private UserSummaryDTO enrolledBy;
    private boolean completed;
    private Instant completedAt;
}
