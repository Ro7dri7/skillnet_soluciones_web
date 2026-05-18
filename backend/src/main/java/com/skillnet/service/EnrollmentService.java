package com.skillnet.service;

import com.skillnet.web.dto.request.EnrollmentRequestDTO;
import com.skillnet.web.dto.response.EnrollmentResponseDTO;
import java.util.List;
import java.util.Optional;

public interface EnrollmentService {

    EnrollmentResponseDTO create(EnrollmentRequestDTO dto);

    Optional<EnrollmentResponseDTO> update(Long id, EnrollmentRequestDTO dto);

    void deleteById(Long id);

    Optional<EnrollmentResponseDTO> findById(Long id);

    List<EnrollmentResponseDTO> findAll();

    Optional<EnrollmentResponseDTO> findByUserIdAndCourseId(Long userId, Long courseId);

    List<EnrollmentResponseDTO> findByUserId(Long userId);

    List<EnrollmentResponseDTO> findByCourseId(Long courseId);
}
