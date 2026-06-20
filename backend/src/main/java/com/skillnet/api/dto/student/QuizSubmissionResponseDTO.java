package com.skillnet.api.dto.student;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class QuizSubmissionResponseDTO {

    private Long id;
    private Long quizId;
    private Long lessonId;
    private int score;
    private String reviewStatus;
    private Integer timeTakenSeconds;
    private Instant createdAt;
}
