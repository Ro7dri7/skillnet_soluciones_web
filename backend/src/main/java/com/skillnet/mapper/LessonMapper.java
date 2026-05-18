package com.skillnet.mapper;

import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.Lesson;
import com.skillnet.persistence.entity.core.Section;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.SectionRepository;
import com.skillnet.web.dto.request.LessonRequestDTO;
import com.skillnet.web.dto.response.LessonResponseDTO;
import com.skillnet.web.dto.response.SectionSummaryDTO;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class LessonMapper {

    private final CourseMapper courseMapper;
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;

    public LessonMapper(
            CourseMapper courseMapper,
            CourseRepository courseRepository,
            SectionRepository sectionRepository) {
        this.courseMapper = courseMapper;
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
    }

    public LessonResponseDTO toResponseDTO(Lesson lesson) {
        if (lesson == null) {
            return null;
        }
        LessonResponseDTO dto = new LessonResponseDTO();
        dto.setId(lesson.getId());
        Course course = lesson.getCourse();
        if (course != null) {
            dto.setCourseId(course.getId());
            dto.setCourse(courseMapper.toSummaryDTO(course));
        }
        Section section = lesson.getSection();
        if (section != null) {
            dto.setSectionId(section.getId());
            dto.setSection(toSectionSummaryDTO(section));
        }
        dto.setTitle(lesson.getTitle());
        dto.setContent(lesson.getContent());
        dto.setResourceUrl(lesson.getResourceUrl());
        dto.setResourceFile(lesson.getResourceFile());
        dto.setContentType(lesson.getContentType());
        dto.setOrderIndex(lesson.getOrderIndex());
        dto.setStatus(lesson.getStatus());
        dto.setVersion(lesson.getVersion());
        dto.setUpdatedAt(lesson.getUpdatedAt());
        dto.setSecurityStatus(lesson.getSecurityStatus());
        dto.setSecurityScanReport(lesson.getSecurityScanReport());
        dto.setLastScannedAt(lesson.getLastScannedAt());
        return dto;
    }

    public SectionSummaryDTO toSectionSummaryDTO(Section section) {
        if (section == null) {
            return null;
        }
        return new SectionSummaryDTO(section.getId(), section.getName(), section.getOrderIndex());
    }

    public Lesson toEntity(LessonRequestDTO dto) {
        Lesson lesson = new Lesson();
        applyToEntity(lesson, dto, true);
        return lesson;
    }

    public void applyToEntity(Lesson lesson, LessonRequestDTO dto, boolean isCreate) {
        if (dto.getCourseId() != null || isCreate) {
            lesson.setCourse(resolveCourse(dto.getCourseId()));
        }
        if (dto.getSectionId() != null) {
            lesson.setSection(resolveSection(dto.getSectionId()));
        } else if (isCreate) {
            lesson.setSection(null);
        }
        if (dto.getTitle() != null) {
            lesson.setTitle(dto.getTitle());
        }
        lesson.setContent(dto.getContent());
        lesson.setResourceUrl(dto.getResourceUrl());
        lesson.setResourceFile(dto.getResourceFile());
        if (dto.getContentType() != null) {
            lesson.setContentType(dto.getContentType());
        }
        lesson.setOrderIndex(dto.getOrderIndex());
        if (dto.getStatus() != null) {
            lesson.setStatus(dto.getStatus());
        }
        if (dto.getVersion() > 0 || !isCreate) {
            lesson.setVersion(dto.getVersion());
        }
        if (dto.getUpdatedAt() != null) {
            lesson.setUpdatedAt(dto.getUpdatedAt());
        } else if (isCreate) {
            lesson.setUpdatedAt(Instant.now());
        }
        if (dto.getSecurityStatus() != null) {
            lesson.setSecurityStatus(dto.getSecurityStatus());
        }
        if (dto.getSecurityScanReport() != null) {
            lesson.setSecurityScanReport(dto.getSecurityScanReport());
        }
        lesson.setLastScannedAt(dto.getLastScannedAt());
    }

    private Course resolveCourse(Long courseId) {
        if (courseId == null) {
            return null;
        }
        return courseRepository.findById(courseId).orElse(null);
    }

    private Section resolveSection(Long sectionId) {
        if (sectionId == null) {
            return null;
        }
        return sectionRepository.findById(sectionId).orElse(null);
    }
}
