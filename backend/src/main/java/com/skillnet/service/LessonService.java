package com.skillnet.service;

import com.skillnet.web.dto.request.LessonRequestDTO;
import com.skillnet.web.dto.response.LessonResponseDTO;
import java.util.List;
import java.util.Optional;

public interface LessonService {

    LessonResponseDTO create(LessonRequestDTO dto);

    Optional<LessonResponseDTO> update(Long id, LessonRequestDTO dto);

    void deleteById(Long id);

    Optional<LessonResponseDTO> findById(Long id);

    List<LessonResponseDTO> findAll();

    List<LessonResponseDTO> findByCourseId(Long courseId);

    List<LessonResponseDTO> findByCourseIdOrdered(Long courseId);
}
