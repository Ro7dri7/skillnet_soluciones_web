package com.skillnet.service;

import com.skillnet.web.dto.request.CourseBuilderRequestDTO;
import com.skillnet.web.dto.response.CourseBuilderResponseDTO;

public interface CourseBuilderService {

    CourseBuilderResponseDTO saveDraft(Long professorId, CourseBuilderRequestDTO request);

    CourseBuilderResponseDTO getByCourseId(Long courseId, Long professorId);
}
