package com.skillnet.web.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CurriculumModuleResponseDTO {

    private Long id;
    private String title;
    private int orderIndex;
    private List<CurriculumLessonResponseDTO> lessons = new ArrayList<>();
}
