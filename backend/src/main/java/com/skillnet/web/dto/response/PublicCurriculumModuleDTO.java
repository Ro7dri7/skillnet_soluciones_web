package com.skillnet.web.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PublicCurriculumModuleDTO {

    private Long id;
    private String title;
    private List<PublicCurriculumLessonDTO> lessons = new ArrayList<>();
}
