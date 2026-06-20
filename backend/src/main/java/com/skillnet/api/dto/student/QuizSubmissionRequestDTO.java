package com.skillnet.api.dto.student;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QuizSubmissionRequestDTO {

    @NotNull
    @Min(0)
    @Max(100)
    private Integer score;

    private Integer timeTakenSeconds;

    private boolean needsManualReview;

    private List<QuizAnswerSubmissionDTO> answers = new ArrayList<>();
}
