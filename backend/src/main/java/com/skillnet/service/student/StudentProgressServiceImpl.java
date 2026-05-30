package com.skillnet.service.student;

import com.skillnet.api.dto.student.LessonProgressResponseDTO;
import com.skillnet.persistence.entity.core.Enrollment;
import com.skillnet.persistence.entity.core.Lesson;
import com.skillnet.persistence.entity.core.Progress;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.persistence.repository.LessonRepository;
import com.skillnet.persistence.repository.ProgressRepository;
import com.skillnet.persistence.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentProgressServiceImpl implements StudentProgressService {

    private final ProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Set<Long> completedLessonIds(Long userId, Long courseId) {
        return progressRepository.findCompletedLessonIdsByUserAndCourse(userId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public int progressPercent(Long userId, Long courseId) {
        long total = lessonRepository.countByCourse_Id(courseId);
        if (total == 0) {
            return 0;
        }
        long done = progressRepository.countCompletedByUserAndCourse(userId, courseId);
        return (int) Math.round((done * 100.0) / total);
    }

    @Override
    @Transactional
    public LessonProgressResponseDTO markLessonComplete(Long userId, Long courseId, Long lessonId) {
        Lesson lesson = lessonRepository
                .findByIdAndCourse_Id(lessonId, courseId)
                .orElseThrow(() -> new EntityNotFoundException("Lesson not found in course: " + lessonId));

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        Progress progress = progressRepository
                .findByUser_IdAndLesson_Id(userId, lessonId)
                .orElseGet(() -> {
                    Progress created = new Progress();
                    created.setUser(user);
                    created.setLesson(lesson);
                    return created;
                });

        progress.setCompleted(true);
        progress.setUpdatedAt(Instant.now());
        progressRepository.save(progress);

        int percent = progressPercent(userId, courseId);
        boolean courseCompleted = syncEnrollmentCompletion(userId, courseId, percent);

        return LessonProgressResponseDTO.builder()
                .lessonId(lessonId)
                .completed(true)
                .progressPercentage(percent)
                .courseCompleted(courseCompleted)
                .build();
    }

    private boolean syncEnrollmentCompletion(Long userId, Long courseId, int percent) {
        if (percent < 100) {
            return false;
        }

        Enrollment enrollment = enrollmentRepository
                .findByUser_IdAndCourse_Id(userId, courseId)
                .orElse(null);
        if (enrollment == null) {
            return false;
        }

        if (!enrollment.isCompleted()) {
            enrollment.setCompleted(true);
            enrollment.setCompletedAt(Instant.now());
            enrollmentRepository.save(enrollment);
        }
        return true;
    }
}
