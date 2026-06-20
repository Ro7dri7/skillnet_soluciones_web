package com.skillnet.service.admin;

import com.skillnet.domain.AuditAction;
import com.skillnet.mapper.EnrollmentMapper;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.Enrollment;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.service.AuditService;
import com.skillnet.service.UserRoleNormalizer;
import com.skillnet.web.dto.request.AdminEnrollmentCreateRequestDTO;
import com.skillnet.web.dto.request.UpdateUserRoleRequestDTO;
import com.skillnet.web.dto.response.EnrollmentResponseDTO;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminEnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> listEnrollments() {
        return enrollmentRepository.findAll().stream()
                .map(enrollmentMapper::toResponseDTO)
                .toList();
    }

    @Transactional
    public EnrollmentResponseDTO createEnrollment(
            AdminEnrollmentCreateRequestDTO dto, String adminEmail) {
        User user = userRepository
                .findById(dto.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Course course = courseRepository
                .findById(dto.getCourseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));

        if (enrollmentRepository.existsByUser_IdAndCourse_Id(user.getId(), course.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya está inscrito");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setEnrolledAt(dto.getEnrolledAt() != null ? dto.getEnrolledAt() : Instant.now());
        enrollment.setEnrollmentType(dto.getEnrollmentType());
        enrollment.setCompleted(false);

        Enrollment saved = enrollmentRepository.save(enrollment);

        auditService.logAction(
                AuditAction.ADMIN_ENROLL_USER,
                AuditAction.ENTITY_ENROLLMENT,
                saved.getId(),
                adminEmail,
                "userId=" + user.getId() + ", courseId=" + course.getId());

        return enrollmentMapper.toResponseDTO(saved);
    }

    @Transactional
    public void updateUserRole(Long userId, UpdateUserRoleRequestDTO dto, String adminEmail) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String previousRole = user.getRole();
        String newRole = dto.getRole().trim().toLowerCase();
        UserRoleNormalizer.applyDualRoleCapabilities(user, newRole);
        userRepository.save(user);

        auditService.logAction(
                AuditAction.ADMIN_CHANGE_USER_ROLE,
                AuditAction.ENTITY_USER,
                userId,
                adminEmail,
                "previousRole=" + previousRole + ", newRole=" + newRole);
    }
}
