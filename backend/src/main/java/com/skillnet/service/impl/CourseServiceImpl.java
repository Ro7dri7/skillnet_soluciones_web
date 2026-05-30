package com.skillnet.service.impl;

import com.skillnet.mapper.CourseMapper;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.CourseRepository;
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

    public CourseServiceImpl(CourseRepository courseRepository, CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
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
        return courseMapper.toResponseDTO(courseRepository.save(course));
    }

    @Override
    @Transactional
    public Optional<CourseResponseDTO> update(Long id, CourseRequestDTO dto) {
        return courseRepository.findById(id).map(existing -> {
            courseMapper.applyToEntity(existing, dto);
            return courseMapper.toResponseDTO(courseRepository.save(existing));
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
        return courseRepository.findById(id).map(courseMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> findAll() {
        return courseRepository.findAll().stream().map(courseMapper::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseResponseDTO> findBySlug(String slug) {
        return courseRepository.findBySlug(slug).map(courseMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseResponseDTO> findBySlugVariants(String slug) {
        return CourseSlugUtils.resolveCourse(courseRepository, slug).map(courseMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> findByProfessorId(Long professorId) {
        return courseRepository.findByProfessor_Id(professorId).stream()
                .map(courseMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> findByStatus(String status) {
        return courseRepository.findByStatus(status).stream()
                .map(courseMapper::toResponseDTO)
                .toList();
    }
}
