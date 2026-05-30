package com.skillnet.service.student;

import com.skillnet.api.dto.student.CourseLearnResponseDTO;
import com.skillnet.api.dto.student.MyCourseResponseDTO;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.Enrollment;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.service.CurriculumService;
import com.skillnet.util.CourseSlugUtils;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentLearningServiceImpl implements StudentLearningService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CurriculumService curriculumService;

    @Override
    @Transactional(readOnly = true)
    public List<MyCourseResponseDTO> getMyCourses(Long userId) {
        return enrollmentRepository.findByUser_IdWithCourseOrderByEnrolledAtDesc(userId).stream()
                .map(this::toMyCourseResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseLearnResponseDTO getLearnPage(String slug, Long userId) {
        Course course = CourseSlugUtils.resolveCourse(courseRepository, slug)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with slug: " + slug));

        if (!canAccessCourse(course, userId)) {
            throw new AccessDeniedException("No tienes acceso a este curso");
        }

        return CourseLearnResponseDTO.builder()
                .courseId(course.getId())
                .title(course.getTitle())
                .slug(CourseSlugUtils.normalizeIsoYearInSlug(course.getSlug()))
                .welcomeMessage(course.getWelcomeMessage())
                .congratulationsMessage(course.getCongratulationsMessage())
                .modules(curriculumService.getCurriculumForLearner(course.getId(), userId))
                .build();
    }

    private boolean canAccessCourse(Course course, Long userId) {
        User professor = course.getProfessor();
        if (professor != null && professor.getId().equals(userId)) {
            return true;
        }
        return enrollmentRepository.existsByUser_IdAndCourse_Id(userId, course.getId());
    }

    private MyCourseResponseDTO toMyCourseResponse(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        User professor = course != null ? course.getProfessor() : null;

        return MyCourseResponseDTO.builder()
                .courseId(course != null ? course.getId() : null)
                .title(course != null ? course.getTitle() : null)
                .slug(course != null ? CourseSlugUtils.normalizeIsoYearInSlug(course.getSlug()) : null)
                .thumbnailUrl(course != null ? course.getImageUrl() : null)
                .authorName(formatAuthorName(professor))
                .progressPercentage(enrollment.isCompleted() ? 100 : 0)
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }

    private String formatAuthorName(User professor) {
        if (professor == null) {
            return "Skillnet";
        }
        String fullName = String.join(
                        " ",
                        professor.getFirstName() != null ? professor.getFirstName().trim() : "",
                        professor.getLastName() != null ? professor.getLastName().trim() : "")
                .trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        return professor.getUsername() != null ? professor.getUsername() : "Skillnet";
    }
}
