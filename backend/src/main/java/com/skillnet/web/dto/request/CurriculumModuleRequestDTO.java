package com.skillnet.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CurriculumModuleRequestDTO {

    private Long id;

    @NotBlank(message = "title is required")
    @Size(max = 255)
    private String title;

    @Min(0)
    private int orderIndex;

    @Valid
    private List<CurriculumLessonRequestDTO> lessons = new ArrayList<>();
}
