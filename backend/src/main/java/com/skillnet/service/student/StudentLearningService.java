package com.skillnet.service.student;

import com.skillnet.api.dto.student.MyCourseResponseDTO;
import java.util.List;

public interface StudentLearningService {

    List<MyCourseResponseDTO> getMyCourses(Long userId);
}
