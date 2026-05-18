package com.skillnet.service.impl;

import com.skillnet.mapper.EnrollmentMapper;
import com.skillnet.persistence.entity.core.Enrollment;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.service.EnrollmentService;
import com.skillnet.web.dto.request.EnrollmentRequestDTO;
import com.skillnet.web.dto.response.EnrollmentResponseDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentMapper enrollmentMapper;

    public EnrollmentServiceImpl(
            EnrollmentRepository enrollmentRepository, EnrollmentMapper enrollmentMapper) {
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentMapper = enrollmentMapper;
    }

    @Override
    @Transactional
    public EnrollmentResponseDTO create(EnrollmentRequestDTO dto) {
        Enrollment enrollment = enrollmentMapper.toEntity(dto);
        return enrollmentMapper.toResponseDTO(enrollmentRepository.save(enrollment));
    }

    @Override
    @Transactional
    public Optional<EnrollmentResponseDTO> update(Long id, EnrollmentRequestDTO dto) {
        return enrollmentRepository.findById(id).map(existing -> {
            enrollmentMapper.applyToEntity(existing, dto);
            return enrollmentMapper.toResponseDTO(enrollmentRepository.save(existing));
        });
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        enrollmentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnrollmentResponseDTO> findById(Long id) {
        return enrollmentRepository.findById(id).map(enrollmentMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> findAll() {
        return enrollmentRepository.findAll().stream()
                .map(enrollmentMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnrollmentResponseDTO> findByUserIdAndCourseId(Long userId, Long courseId) {
        return enrollmentRepository
                .findByUser_IdAndCourse_Id(userId, courseId)
                .map(enrollmentMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> findByUserId(Long userId) {
        return enrollmentRepository.findByUser_Id(userId).stream()
                .map(enrollmentMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> findByCourseId(Long courseId) {
        return enrollmentRepository.findByCourse_Id(courseId).stream()
                .map(enrollmentMapper::toResponseDTO)
                .toList();
    }
}
