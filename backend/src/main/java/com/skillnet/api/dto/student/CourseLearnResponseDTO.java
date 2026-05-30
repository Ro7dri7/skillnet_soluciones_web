package com.skillnet.api.dto.student;

import com.skillnet.web.dto.response.CurriculumModuleResponseDTO;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private int progressPercentage;
    @Builder.Default
    private Set<Long> completedLessonIds = new HashSet<>();
}
