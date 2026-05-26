package com.skillnet.web.dto.response;

import com.skillnet.domain.CourseFormat;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseBuilderResponseDTO {

    private Long id;
    private String slug;
    private CourseFormat format;
    private String title;
    private String category;
    private String subcategory;
    private String audience;
    private String status;
    private List<CurriculumModuleResponseDTO> curriculum = new ArrayList<>();
}
