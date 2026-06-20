package com.skillnet.web.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProducerStudentProgressOverviewDTO {
    private final int totalCourses;
    private final int totalStudents;
    private final List<CourseProgressGroupDTO> courses;
}
