package com.skillnet.web.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CourseProgressGroupDTO {
    private final Long id;
    private final String slug;
    private final String title;
    private final String status;
    private final String imageUrl;
    private final int totalStudents;
    private final List<StudentProgressDetailDTO> students;
}
