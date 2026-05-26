package com.skillnet.web.dto.request;

import jakarta.validation.Valid;
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
public class CourseBuilderRequestDTO {

    private String format = "course";

    @NotBlank(message = "title is required")
    @Size(max = 255)
    private String title;

    @Size(max = 100)
    private String category;

    @Size(max = 200)
    private String subcategory;

    private String audience;

    @Valid
    private List<CurriculumModuleRequestDTO> curriculum = new ArrayList<>();
}
