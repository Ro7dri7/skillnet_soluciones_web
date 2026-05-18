package com.skillnet.service;

import com.skillnet.web.dto.request.CourseRequestDTO;
import com.skillnet.web.dto.response.CourseResponseDTO;
import java.util.List;
import java.util.Optional;

public interface CourseService {

    CourseResponseDTO create(CourseRequestDTO dto);

    Optional<CourseResponseDTO> update(Long id, CourseRequestDTO dto);

    void deleteCourse(Long courseId, Long currentUserId, String currentUserRole);

    Optional<CourseResponseDTO> findById(Long id);

    List<CourseResponseDTO> findAll();

    Optional<CourseResponseDTO> findBySlug(String slug);

    List<CourseResponseDTO> findByProfessorId(Long professorId);

    List<CourseResponseDTO> findByStatus(String status);
}
