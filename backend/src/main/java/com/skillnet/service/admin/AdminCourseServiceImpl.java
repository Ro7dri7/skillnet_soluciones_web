package com.skillnet.service.admin;

import com.skillnet.domain.CourseStatus;
import com.skillnet.mapper.CourseMapper;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.repository.CourseRepository;
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
        return courseMapper.toResponseDTO(courseRepository.save(course));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
