package com.skillnet.service.student;

import com.skillnet.api.dto.student.CourseLearnResponseDTO;
import com.skillnet.api.dto.student.LessonProgressResponseDTO;
import com.skillnet.api.dto.student.MyCourseResponseDTO;
import java.util.List;

public interface StudentLearningService {

    List<MyCourseResponseDTO> getMyCourses(Long userId);

    CourseLearnResponseDTO getLearnPage(String slug, Long userId);

    LessonProgressResponseDTO markLessonComplete(String slug, Long lessonId, Long userId);
}
