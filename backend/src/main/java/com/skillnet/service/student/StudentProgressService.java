package com.skillnet.service.student;

import com.skillnet.api.dto.student.LessonProgressResponseDTO;
import java.util.Set;

public interface StudentProgressService {

    Set<Long> completedLessonIds(Long userId, Long courseId);

    int progressPercent(Long userId, Long courseId);

    LessonProgressResponseDTO markLessonComplete(Long userId, Long courseId, Long lessonId);
}
