package com.skillnet.mapper;

import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.Enrollment;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.web.dto.request.EnrollmentRequestDTO;
import com.skillnet.web.dto.response.EnrollmentResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentMapper {

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public EnrollmentMapper(
            UserMapper userMapper,
            CourseMapper courseMapper,
            UserRepository userRepository,
            CourseRepository courseRepository) {
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    public EnrollmentResponseDTO toResponseDTO(Enrollment enrollment) {
        if (enrollment == null) {
            return null;
        }
        EnrollmentResponseDTO dto = new EnrollmentResponseDTO();
        dto.setId(enrollment.getId());
        User user = enrollment.getUser();
        if (user != null) {
            dto.setUserId(user.getId());
            dto.setUser(userMapper.toSummaryDTO(user));
        }
        Course course = enrollment.getCourse();
        if (course != null) {
            dto.setCourseId(course.getId());
            dto.setCourse(courseMapper.toSummaryDTO(course));
        }
        dto.setEnrolledAt(enrollment.getEnrolledAt());
        dto.setEnrollmentType(enrollment.getEnrollmentType());
        User enrolledBy = enrollment.getEnrolledBy();
        if (enrolledBy != null) {
            dto.setEnrolledById(enrolledBy.getId());
            dto.setEnrolledBy(userMapper.toSummaryDTO(enrolledBy));
        }
        dto.setCompleted(enrollment.isCompleted());
        dto.setCompletedAt(enrollment.getCompletedAt());
        return dto;
    }

    public Enrollment toEntity(EnrollmentRequestDTO dto) {
        Enrollment enrollment = new Enrollment();
        applyToEntity(enrollment, dto);
        return enrollment;
    }

    public void applyToEntity(Enrollment enrollment, EnrollmentRequestDTO dto) {
        if (dto.getUserId() != null) {
            enrollment.setUser(resolveUser(dto.getUserId()));
        }
        if (dto.getCourseId() != null) {
            enrollment.setCourse(resolveCourse(dto.getCourseId()));
        }
        if (dto.getEnrolledAt() != null) {
            enrollment.setEnrolledAt(dto.getEnrolledAt());
        }
        if (dto.getEnrollmentType() != null) {
            enrollment.setEnrollmentType(dto.getEnrollmentType());
        }
        if (dto.getEnrolledById() != null) {
            enrollment.setEnrolledBy(resolveUser(dto.getEnrolledById()));
        }
        enrollment.setCompleted(dto.isCompleted());
        if (dto.getCompletedAt() != null) {
            enrollment.setCompletedAt(dto.getCompletedAt());
        }
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private Course resolveCourse(Long courseId) {
        if (courseId == null) {
            return null;
        }
        return courseRepository.findById(courseId).orElse(null);
    }
}
