package com.skillnet.api.dto.student;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class QuizAttemptsResponseDTO {

    private Long quizId;
    private int attemptsUsed;
    private int maxAttempts;
    private int attemptsRemaining;
    private boolean canSubmit;
}
