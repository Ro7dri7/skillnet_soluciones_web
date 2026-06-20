package com.skillnet.service.producer;

import com.skillnet.domain.CourseStatus;
import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.Enrollment;
import com.skillnet.persistence.entity.core.Lesson;
import com.skillnet.persistence.entity.core.Quiz;
import com.skillnet.persistence.entity.core.QuizSubmission;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.persistence.repository.LessonRepository;
import com.skillnet.persistence.repository.ProgressRepository;
import com.skillnet.persistence.repository.QuizRepository;
import com.skillnet.persistence.repository.QuizSubmissionRepository;
import com.skillnet.service.media.MediaStorageService;
import com.skillnet.web.dto.response.CourseProgressGroupDTO;
import com.skillnet.web.dto.response.ProducerStudentProgressOverviewDTO;
import com.skillnet.web.dto.response.QuizProgressItemDTO;
import com.skillnet.web.dto.response.StudentProgressDetailDTO;
import com.skillnet.web.dto.response.StudentProgressItemDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProducerStudentProgressService {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final ProgressRepository progressRepository;
    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final MediaStorageService mediaStorageService;

    @Transactional(readOnly = true)
    public List<StudentProgressItemDTO> getStudentProgress(Long professorId, Long courseId) {
        List<Enrollment> enrollments;
        if (courseId != null) {
            enrollments = enrollmentRepository.findByCourse_Id(courseId).stream()
                    .filter(e -> e.getCourse().getProfessor() != null
                            && professorId.equals(e.getCourse().getProfessor().getId()))
                    .toList();
        } else {
            enrollments = enrollmentRepository.findAll().stream()
                    .filter(e -> e.getCourse().getProfessor() != null
                            && professorId.equals(e.getCourse().getProfessor().getId()))
                    .toList();
        }

        return enrollments.stream().map(this::toProgressItem).toList();
    }

    @Transactional(readOnly = true)
    public ProducerStudentProgressOverviewDTO getOverview(Long professorId, String courseSlug) {
        List<Course> courses = courseRepository.findByProfessor_Id(professorId).stream()
                .filter(c -> !CourseStatus.DELETED.getDbValue().equalsIgnoreCase(c.getStatus()))
                .filter(c -> CourseStatus.PUBLISHED.getDbValue().equalsIgnoreCase(c.getStatus()))
                .sorted(Comparator.comparing(Course::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<CourseProgressGroupDTO> grouped = new ArrayList<>();
        int totalStudents = 0;

        for (Course course : courses) {
            List<Enrollment> enrollments = enrollmentRepository.findByCourse_Id(course.getId());
            List<Lesson> lessons =
                    lessonRepository.findByCourse_IdAndStatus(course.getId(), "published");
            List<Quiz> quizzes = quizRepository.findByCourse_Id(course.getId());

            List<StudentProgressDetailDTO> students = enrollments.stream()
                    .map(enrollment -> toDetail(enrollment, lessons, quizzes))
                    .toList();

            totalStudents += students.size();
            grouped.add(CourseProgressGroupDTO.builder()
                    .id(course.getId())
                    .slug(course.getSlug())
                    .title(course.getTitle())
                    .status(course.getStatus())
                    .imageUrl(mediaStorageService.resolveCourseImageUrl(course.getImageUrl(), course.getImageFile()))
                    .totalStudents(students.size())
                    .students(students)
                    .build());
        }

        return ProducerStudentProgressOverviewDTO.builder()
                .totalCourses(grouped.size())
                .totalStudents(totalStudents)
                .courses(grouped)
                .build();
    }

    private StudentProgressDetailDTO toDetail(Enrollment enrollment, List<Lesson> lessons, List<Quiz> quizzes) {
        Long courseId = enrollment.getCourse().getId();
        Long userId = enrollment.getUser().getId();
        long totalLessons = lessons.size();
        Set<Long> completedLessonIds = progressRepository.findCompletedLessonIdsByUserAndCourse(userId, courseId);
        long completedLessons = completedLessonIds.size();
        double progressPercent = totalLessons == 0 ? 0.0 : (completedLessons * 100.0) / totalLessons;

        User user = enrollment.getUser();
        String userName = formatName(user);

        List<QuizProgressItemDTO> quizItems = buildQuizProgress(userId, lessons, quizzes);

        return StudentProgressDetailDTO.builder()
                .enrollmentId(enrollment.getId())
                .userId(userId)
                .userName(userName)
                .userEmail(user.getEmail())
                .profilePictureUrl(user.getProfilePicture())
                .enrolledAt(enrollment.getEnrolledAt())
                .completed(enrollment.isCompleted())
                .completedLessons((int) completedLessons)
                .totalLessons((int) totalLessons)
                .progressPercent(progressPercent)
                .quizzes(quizItems)
                .build();
    }

    private List<QuizProgressItemDTO> buildQuizProgress(Long userId, List<Lesson> lessons, List<Quiz> quizzes) {
        java.util.Map<Long, QuizSubmission> latestByQuiz = new java.util.HashMap<>();
        for (Quiz quiz : quizzes) {
            quizSubmissionRepository.findByUser_IdAndQuiz_IdOrderByCreatedAtDesc(userId, quiz.getId()).stream()
                    .findFirst()
                    .ifPresent(submission -> latestByQuiz.put(quiz.getId(), submission));
        }

        List<QuizProgressItemDTO> items = new ArrayList<>();
        for (Quiz quiz : quizzes) {
            items.add(toQuizItem(quiz, latestByQuiz.get(quiz.getId()), null));
        }

        for (Lesson lesson : lessons) {
            if (!"quiz".equalsIgnoreCase(lesson.getContentType())) {
                continue;
            }
            Quiz linked = quizzes.stream()
                    .filter(q -> q.getSection() != null
                            && lesson.getSection() != null
                            && q.getSection().getId().equals(lesson.getSection().getId())
                            && q.getOrderIndex() == lesson.getOrderIndex())
                    .findFirst()
                    .orElse(null);
            if (linked != null) {
                continue;
            }
            items.add(toQuizItem(null, null, lesson));
        }
        return items;
    }

    private QuizProgressItemDTO toQuizItem(Quiz quiz, QuizSubmission latest, Lesson lesson) {
        String title = quiz != null ? quiz.getTitle() : lesson != null ? lesson.getTitle() : "Quiz";
        Long quizId = quiz != null ? quiz.getId() : null;
        Long lessonId = lesson != null ? lesson.getId() : null;
        if (latest == null) {
            return QuizProgressItemDTO.builder()
                    .lessonId(lessonId)
                    .quizId(quizId)
                    .title(title)
                    .status("not_started")
                    .color("#94a3b8")
                    .build();
        }
        String status = latest.getReviewStatus() != null ? latest.getReviewStatus().toLowerCase(Locale.ROOT) : "pending";
        String color =
                switch (status) {
                    case "approved" -> "#10b981";
                    case "rejected" -> "#ef4444";
                    default -> "#f59e0b";
                };
        return QuizProgressItemDTO.builder()
                .lessonId(lessonId)
                .quizId(quizId)
                .submissionId(latest.getId())
                .title(title)
                .status(status)
                .color(color)
                .score(latest.getScore())
                .build();
    }

    private StudentProgressItemDTO toProgressItem(Enrollment enrollment) {
        Long courseId = enrollment.getCourse().getId();
        Long userId = enrollment.getUser().getId();
        long totalLessons = lessonRepository.countByCourse_Id(courseId);
        long completedLessons = progressRepository.countCompletedByUserAndCourse(userId, courseId);
        double progressPercent = totalLessons == 0 ? 0.0 : (completedLessons * 100.0) / totalLessons;

        User user = enrollment.getUser();
        return StudentProgressItemDTO.builder()
                .enrollmentId(enrollment.getId())
                .userId(userId)
                .userName(formatName(user))
                .userEmail(user.getEmail())
                .courseId(courseId)
                .courseTitle(enrollment.getCourse().getTitle())
                .enrolledAt(enrollment.getEnrolledAt())
                .completed(enrollment.isCompleted())
                .completedAt(enrollment.getCompletedAt())
                .completedLessons(completedLessons)
                .totalLessons(totalLessons)
                .progressPercent(progressPercent)
                .build();
    }

    private String formatName(User user) {
        String userName = ((user.getFirstName() != null ? user.getFirstName() : "")
                        + " "
                        + (user.getLastName() != null ? user.getLastName() : ""))
                .trim();
        return userName.isBlank() ? user.getUsername() : userName;
    }
}
