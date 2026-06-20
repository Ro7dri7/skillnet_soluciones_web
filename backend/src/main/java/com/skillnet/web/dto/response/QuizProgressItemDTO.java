package com.skillnet.web.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizProgressItemDTO {
    private final Long lessonId;
    private final Long quizId;
    private final Long submissionId;
    private final String title;
    private final String status;
    private final String color;
    private final Integer score;
}
