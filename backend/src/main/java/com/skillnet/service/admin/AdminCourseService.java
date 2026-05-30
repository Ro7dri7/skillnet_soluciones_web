package com.skillnet.service.admin;

import com.skillnet.web.dto.response.CourseResponseDTO;
import java.util.List;

public interface AdminCourseService {

    List<CourseResponseDTO> listCourses();

    CourseResponseDTO publishCourse(Long courseId);

    CourseResponseDTO setDraft(Long courseId);

    CourseResponseDTO takedownCourse(Long courseId);
}
