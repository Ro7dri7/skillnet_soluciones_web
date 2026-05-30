package com.skillnet.api.dto.student;

import com.skillnet.web.dto.response.CurriculumModuleResponseDTO;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CourseLearnResponseDTO {

    private Long courseId;
    private String title;
    private String slug;
    private String welcomeMessage;
    private String congratulationsMessage;
    @Builder.Default
    private List<CurriculumModuleResponseDTO> modules = new ArrayList<>();
}
