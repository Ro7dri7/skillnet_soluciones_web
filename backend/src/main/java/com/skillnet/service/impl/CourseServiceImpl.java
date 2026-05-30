package com.skillnet.service.impl;

import com.skillnet.mapper.CourseMapper;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.persistence.repository.LessonRepository;
import com.skillnet.persistence.repository.SectionRepository;
import com.skillnet.service.CourseService;
import com.skillnet.util.CourseSlugUtils;
import com.skillnet.web.dto.request.CourseRequestDTO;
import com.skillnet.web.dto.response.CourseResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;

    public CourseServiceImpl(
            CourseRepository courseRepository,
            CourseMapper courseMapper,
            SectionRepository sectionRepository,
            LessonRepository lessonRepository,
            EnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    @Transactional
    public CourseResponseDTO create(CourseRequestDTO dto) {
        Course course = courseMapper.toEntity(dto);
        if (course.getSlug() == null || course.getSlug().isBlank()) {
            course.setSlug(CourseSlugUtils.uniqueSlug(courseRepository, dto.getTitle(), null));
        } else {
            course.setSlug(CourseSlugUtils.normalizeIsoYearInSlug(course.getSlug().trim()));
        }
        return enrichPublicStats(courseMapper.toResponseDTO(courseRepository.save(course)));
    }

    @Override
    @Transactional
    public Optional<CourseResponseDTO> update(Long id, CourseRequestDTO dto) {
        return courseRepository.findById(id).map(existing -> {
            courseMapper.applyToEntity(existing, dto);
            return enrichPublicStats(courseMapper.toResponseDTO(courseRepository.save(existing)));
        });
    }

    @Override
    @Transactional
    public void deleteCourse(Long courseId, Long currentUserId, String currentUserRole) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        boolean isAdmin = currentUserRole != null && "admin".equalsIgnoreCase(currentUserRole.trim());
        if (!isAdmin) {
            User professor = course.getProfessor();
            if (professor == null || !professor.getId().equals(currentUserId)) {
                throw new AccessDeniedException("No tienes permiso para eliminar este curso");
            }
        }

        courseRepository.delete(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseResponseDTO> findById(Long id) {
        return courseRepository.findById(id).map(this::mapWithPublicStats);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> findAll() {
        return courseRepository.findAll().stream().map(this::mapWithPublicStats).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseResponseDTO> findBySlug(String slug) {
        return courseRepository.findBySlug(slug).map(this::mapWithPublicStats);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseResponseDTO> findBySlugVariants(String slug) {
        return CourseSlugUtils.resolveCourse(courseRepository, slug).map(this::mapWithPublicStats);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> findByProfessorId(Long professorId) {
        return courseRepository.findByProfessor_Id(professorId).stream()
                .map(this::mapWithPublicStats)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> findByStatus(String status) {
        return courseRepository.findByStatus(status).stream()
                .map(this::mapWithPublicStats)
                .toList();
    }

    private CourseResponseDTO mapWithPublicStats(Course course) {
        return enrichPublicStats(courseMapper.toResponseDTO(course));
    }

    private CourseResponseDTO enrichPublicStats(CourseResponseDTO dto) {
        if (dto == null || dto.getId() == null) {
            return dto;
        }
        long courseId = dto.getId();
        dto.setModuleCount((int) sectionRepository.countByCourse_Id(courseId));
        dto.setLessonsCount((int) lessonRepository.countByCourse_Id(courseId));
        dto.setEnrollmentCount(enrollmentRepository.countByCourse_Id(courseId));
        return dto;
    }
}
