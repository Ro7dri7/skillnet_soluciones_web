package com.skillnet.web.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentProgressDetailDTO {
    private final Long userId;
    private final String userName;
    private final String userEmail;
    private final String profilePictureUrl;
    private final Long enrollmentId;
    private final Instant enrolledAt;
    private final boolean completed;
    private final double progressPercent;
    private final int completedLessons;
    private final int totalLessons;
    private final List<QuizProgressItemDTO> quizzes;
}
