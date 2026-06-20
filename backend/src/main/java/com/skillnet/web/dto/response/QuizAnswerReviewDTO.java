package com.skillnet.web.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizAnswerReviewDTO {
    private final Long id;
    private final Long questionId;
    private final String questionText;
    private final String questionType;
    private final String textAnswer;
    private final Boolean correct;
    private final String tutorFeedback;
    private final boolean requiresManualGrading;
}
