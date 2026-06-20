package com.skillnet.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GradeQuizAnswerRequestDTO {
    @NotNull
    private Long answerId;

    @NotNull
    private Boolean correct;

    private String feedback;
}
