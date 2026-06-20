package com.skillnet.web.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GradeQuizAnswerResponseDTO {
    private final Long answerId;
    private final Boolean correct;
    private final int newScore;
    private final String reviewStatus;
    private final String message;
}
