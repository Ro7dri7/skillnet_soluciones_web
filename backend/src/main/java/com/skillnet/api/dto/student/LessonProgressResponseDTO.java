package com.skillnet.api.dto.student;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LessonProgressResponseDTO {

    private Long lessonId;
    private boolean completed;
    private int progressPercentage;
    private boolean courseCompleted;
}
