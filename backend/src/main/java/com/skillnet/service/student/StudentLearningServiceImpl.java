package com.skillnet.service.student;

import com.skillnet.api.dto.student.MyCourseResponseDTO;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.Enrollment;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.EnrollmentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentLearningServiceImpl implements StudentLearningService {

    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MyCourseResponseDTO> getMyCourses(Long userId) {
        return enrollmentRepository.findByUser_IdWithCourseOrderByEnrolledAtDesc(userId).stream()
                .map(this::toMyCourseResponse)
                .toList();
    }

    private MyCourseResponseDTO toMyCourseResponse(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        User professor = course != null ? course.getProfessor() : null;

        return MyCourseResponseDTO.builder()
                .courseId(course != null ? course.getId() : null)
                .title(course != null ? course.getTitle() : null)
                .slug(course != null ? course.getSlug() : null)
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
