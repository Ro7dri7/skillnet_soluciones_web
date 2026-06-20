package com.skillnet.service.impl;

import com.skillnet.domain.AuditAction;
import com.skillnet.mapper.CourseMapper;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.Lesson;
import com.skillnet.persistence.entity.core.Section;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.persistence.repository.LessonRepository;
import com.skillnet.persistence.repository.SectionRepository;
import com.skillnet.service.AuditService;
import com.skillnet.service.CourseService;
import com.skillnet.util.CourseSlugUtils;
import com.skillnet.web.dto.request.CourseRequestDTO;
import com.skillnet.web.dto.response.CourseResponseDTO;
import com.skillnet.web.dto.response.PublicCurriculumLessonDTO;
import com.skillnet.web.dto.response.PublicCurriculumModuleDTO;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
    private final AuditService auditService;

    public CourseServiceImpl(
            CourseRepository courseRepository,
            CourseMapper courseMapper,
            SectionRepository sectionRepository,
            LessonRepository lessonRepository,
            EnrollmentRepository enrollmentRepository,
            AuditService auditService) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public CourseResponseDTO create(CourseRequestDTO dto) {
        Course course = courseMapper.toEntity(dto);
        if (course.getSlug() == null || course.getSlug().isBlank()) {
            course.setSlug(CourseSlugUtils.uniqueSlug(courseRepository, dto.getTitle(), course.getFormat(), null));
        } else {
            course.setSlug(CourseSlugUtils.normalizeIsoYearInSlug(course.getSlug().trim()));
        }
        Course saved = courseRepository.save(course);
        auditService.logAction(
                AuditAction.CREATE_COURSE,
                AuditAction.ENTITY_COURSE,
                saved.getId(),
                null,
                "Curso creado: " + saved.getTitle());
        return enrichPublicStats(courseMapper.toResponseDTO(saved));
    }

    @Override
    @Transactional
    public Optional<CourseResponseDTO> update(Long id, CourseRequestDTO dto) {
        return courseRepository.findById(id).map(existing -> {
            courseMapper.applyToEntity(existing, dto);
            Course saved = courseRepository.save(existing);
            auditService.logAction(
                    AuditAction.UPDATE_COURSE,
                    AuditAction.ENTITY_COURSE,
                    saved.getId(),
                    null,
                    "Curso actualizado: " + saved.getTitle());
            return enrichPublicStats(courseMapper.toResponseDTO(saved));
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

        auditService.logAction(
                AuditAction.DELETE_COURSE,
                AuditAction.ENTITY_COURSE,
                courseId,
                null,
                "Curso eliminado: " + course.getTitle());
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
        return CourseSlugUtils.resolveCourse(courseRepository, slug).map(course -> {
            CourseResponseDTO dto = mapWithPublicStats(course);
            dto.setSections(buildPublicCurriculum(course.getId()));
            return dto;
        });
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

    private List<PublicCurriculumModuleDTO> buildPublicCurriculum(long courseId) {
        return sectionRepository.findByCourse_IdOrderByOrderIndexAsc(courseId).stream()
                .map(this::toPublicModule)
                .collect(Collectors.toList());
    }

    private PublicCurriculumModuleDTO toPublicModule(Section section) {
        PublicCurriculumModuleDTO module = new PublicCurriculumModuleDTO();
        module.setId(section.getId());
        module.setTitle(section.getTitle());
        List<Lesson> lessons = lessonRepository.findBySection_IdOrderByOrderIndexAsc(section.getId());
        module.setLessons(lessons.stream()
                .map(lesson -> new PublicCurriculumLessonDTO(lesson.getId(), lesson.getTitle()))
                .toList());
        return module;
    }
}
