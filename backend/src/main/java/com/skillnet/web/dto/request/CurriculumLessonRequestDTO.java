package com.skillnet.web.dto.request;

import com.skillnet.domain.LessonType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CurriculumLessonRequestDTO {

    private Long id;

    @NotBlank(message = "title is required")
    @Size(max = 255)
    private String title;

    @Size(max = 1000)
    private String contentUrl;

    private LessonType lessonType = LessonType.VIDEO;

    @Min(0)
    private int durationMinutes;

    @Min(0)
    private int orderIndex;
}
