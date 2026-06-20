package com.skillnet.web.dto.response;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StudentProgressItemDTO {

    private Long enrollmentId;
    private Long userId;
    private String userName;
    private String userEmail;
    private Long courseId;
    private String courseTitle;
    private Instant enrolledAt;
    private boolean completed;
    private Instant completedAt;
    private long completedLessons;
    private long totalLessons;
    private double progressPercent;
}
