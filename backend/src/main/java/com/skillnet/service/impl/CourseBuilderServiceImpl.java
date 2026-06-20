package com.skillnet.service.impl;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.skillnet.domain.CourseFormat;
import com.skillnet.domain.CourseStatus;
import com.skillnet.domain.LessonType;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.Lesson;
import com.skillnet.persistence.entity.core.Section;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.LessonRepository;
import com.skillnet.persistence.repository.SectionRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.service.CourseBuilderService;
import com.skillnet.util.CourseSlugUtils;
import com.skillnet.web.dto.request.CourseBuilderRequestDTO;
import com.skillnet.web.dto.request.CurriculumLessonRequestDTO;
import com.skillnet.web.dto.request.CurriculumModuleRequestDTO;
import com.skillnet.web.dto.response.CourseBuilderResponseDTO;
import com.skillnet.web.dto.response.CurriculumLessonResponseDTO;
import com.skillnet.web.dto.response.CurriculumModuleResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseBuilderServiceImpl implements CourseBuilderService {

    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    public CourseBuilderServiceImpl(
            CourseRepository courseRepository,
            SectionRepository sectionRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public CourseBuilderResponseDTO saveDraft(Long professorId, CourseBuilderRequestDTO request) {
        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> new EntityNotFoundException("Professor not found"));

        Course course = new Course();
        course.setProfessor(professor);
        applyBuilderFields(course, request);
        course.setCreatedAt(Instant.now());
        course.setSoftware(JsonNodeFactory.instance.arrayNode());
        course.setOriginalPrice(BigDecimal.ZERO);
        course.setPrice(BigDecimal.ZERO);
        course.setSlug(CourseSlugUtils.uniqueSlug(courseRepository, request.getTitle(), course.getFormat(), null));

        Course saved = courseRepository.save(course);
        persistCurriculum(saved, request.getCurriculum());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseBuilderResponseDTO getByCourseId(Long courseId, Long professorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));
        assertProfessorOwnership(course, professorId);
        return toResponse(course);
    }

    private void applyBuilderFields(Course course, CourseBuilderRequestDTO request) {
        CourseFormat format = CourseFormat.fromDbValue(request.getFormat());
        course.setTitle(request.getTitle().trim());
        course.setFormat(format.getDbValue());
        course.setCategory(request.getCategory());
        course.setSubcategory(request.getSubcategory());
        course.setAudience(request.getAudience());
        course.setStatus(CourseStatus.DRAFT.getDbValue());
    }

    private void persistCurriculum(Course course, List<CurriculumModuleRequestDTO> modules) {
        if (modules == null || modules.isEmpty()) {
            return;
        }

        for (CurriculumModuleRequestDTO moduleDto : modules) {
            Section section = new Section();
            section.setCourse(course);
            section.setTitle(moduleDto.getTitle().trim());
            section.setOrderIndex(moduleDto.getOrderIndex());
            Section savedSection = sectionRepository.save(section);

            if (moduleDto.getLessons() == null) {
                continue;
            }

            for (CurriculumLessonRequestDTO lessonDto : moduleDto.getLessons()) {
                Lesson lesson = new Lesson();
                lesson.setCourse(course);
                lesson.setSection(savedSection);
                lesson.setTitle(lessonDto.getTitle().trim());
                lesson.setResourceUrl(lessonDto.getContentUrl());
                LessonType lessonType = lessonDto.getLessonType() != null ? lessonDto.getLessonType() : LessonType.VIDEO;
                lesson.setContentType(lessonType.getContentType());
                lesson.setContent("{\"uiContentType\":\"" + mapLessonTypeToUi(lessonType) + "\"}");
                lesson.setOrderIndex(lessonDto.getOrderIndex());
                lesson.setStatus(CourseStatus.DRAFT.getDbValue());
                lesson.setUpdatedAt(Instant.now());
                lesson.setSecurityStatus("pending");
                lesson.setSecurityScanReport(JsonNodeFactory.instance.objectNode());
                lessonRepository.save(lesson);
            }
        }
    }

    private CourseBuilderResponseDTO toResponse(Course course) {
        CourseBuilderResponseDTO dto = new CourseBuilderResponseDTO();
        dto.setId(course.getId());
        dto.setSlug(course.getSlug());
        dto.setFormat(CourseFormat.fromDbValue(course.getCourseFormat()));
        dto.setTitle(course.getTitle());
        dto.setCategory(course.getCategory());
        dto.setSubcategory(course.getSubcategory());
        dto.setAudience(course.getTargetAudience());
        dto.setStatus(course.getStatus());

        List<Section> sections = sectionRepository.findByCourse_IdOrderByOrderIndexAsc(course.getId());
        List<CurriculumModuleResponseDTO> modules = new ArrayList<>();
        for (Section section : sections) {
            CurriculumModuleResponseDTO moduleDto = new CurriculumModuleResponseDTO();
            moduleDto.setId(section.getId());
            moduleDto.setTitle(section.getTitle());
            moduleDto.setOrderIndex(section.getOrderIndex());

            List<Lesson> lessons = lessonRepository.findBySection_Id(section.getId());
            lessons.sort((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()));
            for (Lesson lesson : lessons) {
                moduleDto.getLessons().add(toLessonResponse(lesson));
            }
            modules.add(moduleDto);
        }
        dto.setCurriculum(modules);
        return dto;
    }

    private CurriculumLessonResponseDTO toLessonResponse(Lesson lesson) {
        CurriculumLessonResponseDTO dto = new CurriculumLessonResponseDTO();
        dto.setId(lesson.getId());
        dto.setTitle(lesson.getTitle());
        dto.setResourceUrl(lesson.getResourceUrl() != null ? lesson.getResourceUrl() : "");
        dto.setContentType("quiz".equalsIgnoreCase(lesson.getContentType()) ? "quiz" : "video");
        dto.setTextContent(lesson.getContent() != null ? lesson.getContent() : "");
        dto.setOrderIndex(lesson.getOrderIndex());
        return dto;
    }

    private String mapLessonTypeToUi(LessonType type) {
        if (type == LessonType.QUIZ) {
            return "quiz";
        }
        if (type == LessonType.TEXT) {
            return "text";
        }
        return "video";
    }

    private void assertProfessorOwnership(Course course, Long professorId) {
        if (course.getProfessor() == null || !course.getProfessor().getId().equals(professorId)) {
            throw new AccessDeniedException("You do not own this course");
        }
    }

    private String generateUniqueSlug(String title) {
        return CourseSlugUtils.uniqueSlug(courseRepository, title, null);
    }
}
