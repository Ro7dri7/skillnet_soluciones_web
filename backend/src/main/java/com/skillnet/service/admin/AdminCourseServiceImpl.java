package com.skillnet.service.admin;

import com.skillnet.domain.AuditAction;
import com.skillnet.domain.CourseStatus;
import com.skillnet.mapper.CourseMapper;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.service.AuditService;
import com.skillnet.web.dto.response.CourseResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class AdminCourseServiceImpl implements AdminCourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> listCourses() {
        return courseRepository.findAll().stream().map(courseMapper::toResponseDTO).toList();
    }

    @Override
    @Transactional
    public CourseResponseDTO publishCourse(Long courseId) {
        return updateStatus(courseId, CourseStatus.PUBLISHED);
    }

    @Override
    @Transactional
    public CourseResponseDTO setDraft(Long courseId) {
        return updateStatus(courseId, CourseStatus.DRAFT);
    }

    @Override
    @Transactional
    public CourseResponseDTO takedownCourse(Long courseId) {
        return updateStatus(courseId, CourseStatus.DELETED);
    }

    private CourseResponseDTO updateStatus(Long courseId, CourseStatus status) {
        Course course = courseRepository
                .findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        if (status == CourseStatus.PUBLISHED && isBlank(course.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El curso necesita un título para publicarse.");
        }

        course.setStatus(status.getDbValue());
        Course saved = courseRepository.save(course);
        logAdminCourseAction(saved, status);
        return courseMapper.toResponseDTO(saved);
    }

    private void logAdminCourseAction(Course course, CourseStatus status) {
        String action =
                switch (status) {
                    case PUBLISHED -> AuditAction.PUBLISH_COURSE;
                    case DRAFT -> AuditAction.SET_DRAFT_COURSE;
                    case DELETED -> AuditAction.TAKEDOWN_COURSE;
                };
        auditService.logAction(
                action,
                AuditAction.ENTITY_COURSE,
                course.getId(),
                null,
                "Admin cambió estado a " + status.getDbValue() + ": " + course.getTitle());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
