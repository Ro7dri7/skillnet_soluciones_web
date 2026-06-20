package com.skillnet.service.student;

import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.Enrollment;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.CourseCertificateRepository;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.persistence.repository.LessonRepository;
import com.skillnet.persistence.repository.projection.CategoryProgressProjection;
import com.skillnet.persistence.repository.projection.DailyCountProjection;
import com.skillnet.service.media.MediaStorageService;
import com.skillnet.util.CourseSlugUtils;
import com.skillnet.web.dto.response.analytics.CategoryProgressDTO;
import com.skillnet.web.dto.response.analytics.DailyCountDTO;
import com.skillnet.web.dto.response.analytics.StudentAnalyticsDTO;
import com.skillnet.web.dto.response.analytics.StudentKpiDTO;
import com.skillnet.web.dto.response.analytics.StudentLearningCourseDTO;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentAnalyticsServiceImpl implements StudentAnalyticsService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final CourseCertificateRepository courseCertificateRepository;
    private final MediaStorageService mediaStorageService;
    private final StudentProgressService studentProgressService;

    @Override
    @Transactional(readOnly = true)
    public StudentAnalyticsDTO getAnalytics(Long userId, Integer year, Integer month) {
        YearMonth period = resolvePeriod(year, month);
        Instant periodStart = period.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant periodEnd = period.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        StudentAnalyticsDTO analytics = new StudentAnalyticsDTO();
        analytics.setKpis(buildKpis(userId, periodStart, periodEnd));
        analytics.setPurchaseTrend(buildPurchaseTrend(userId, periodStart, periodEnd));
        analytics.setProgressByCategory(buildProgressByCategory(userId, periodStart, periodEnd));
        analytics.setLearningCourses(buildLearningCourses(userId));
        return analytics;
    }

    private StudentKpiDTO buildKpis(Long userId, Instant periodStart, Instant periodEnd) {
        StudentKpiDTO kpis = new StudentKpiDTO();
        kpis.setPurchasedCourses(enrollmentRepository.countByUser_Id(userId));
        kpis.setPurchasedInPeriod(
                enrollmentRepository.countByUser_IdAndEnrolledAtBetween(userId, periodStart, periodEnd));
        long completed = enrollmentRepository.countCompletedAllTimeByUser_Id(userId);
        kpis.setCompletedCourses(completed);
        kpis.setCertificates(courseCertificateRepository.countByStudent_IdAndActiveTrue(userId));
        kpis.setActiveCourses(enrollmentRepository.countActiveByUser_Id(userId));
        return kpis;
    }

    private List<DailyCountDTO> buildPurchaseTrend(Long userId, Instant periodStart, Instant periodEnd) {
        return enrollmentRepository.countDailyEnrollmentsByUserAndPeriod(userId, periodStart, periodEnd).stream()
                .map(this::toDailyCountDto)
                .toList();
    }

    private List<CategoryProgressDTO> buildProgressByCategory(Long userId, Instant periodStart, Instant periodEnd) {
        return enrollmentRepository.progressByCategoryForUserAndPeriod(userId, periodStart, periodEnd).stream()
                .map(this::toCategoryProgressDto)
                .toList();
    }

    private List<StudentLearningCourseDTO> buildLearningCourses(Long userId) {
        return enrollmentRepository.findAllByUser_IdWithCourse(userId).stream()
                .map(this::toLearningCourseDto)
                .toList();
    }

    private DailyCountDTO toDailyCountDto(DailyCountProjection projection) {
        DailyCountDTO dto = new DailyCountDTO();
        LocalDate date = projection.getDate();
        dto.setDate(date != null ? date.format(DATE_FORMAT) : null);
        dto.setCount(projection.getCount() != null ? projection.getCount() : 0L);
        return dto;
    }

    private CategoryProgressDTO toCategoryProgressDto(CategoryProgressProjection projection) {
        CategoryProgressDTO dto = new CategoryProgressDTO();
        dto.setCategoryName(projection.getCategoryName());
        double raw = projection.getPercent() != null ? projection.getPercent() : 0.0;
        dto.setPercent((int) Math.round(raw));
        return dto;
    }

    private StudentLearningCourseDTO toLearningCourseDto(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        User professor = course != null ? course.getProfessor() : null;

        StudentLearningCourseDTO dto = new StudentLearningCourseDTO();
        dto.setId(course != null ? course.getId() : enrollment.getId());
        dto.setTitle(course != null ? course.getTitle() : "Curso");
        dto.setProfessor(formatAuthorName(professor));
        dto.setCategory(course != null && course.getCategory() != null ? course.getCategory() : "General");
        dto.setSlug(course != null ? CourseSlugUtils.normalizeIsoYearInSlug(course.getSlug()) : null);
        dto.setThumbnailUrl(
                course != null
                        ? mediaStorageService.resolveCourseImageUrl(course.getImageUrl(), course.getImageFile())
                        : null);
        int progress = course != null
                ? studentProgressService.progressPercent(enrollment.getUser().getId(), course.getId())
                : (enrollment.isCompleted() ? 100 : 0);
        dto.setProgress(progress);
        long lessonsTotal = course != null ? lessonRepository.countByCourse_Id(course.getId()) : 0;
        long lessonsDone = course != null
                ? studentProgressService
                        .completedLessonIds(enrollment.getUser().getId(), course.getId())
                        .size()
                : 0;
        dto.setLessonsDone((int) lessonsDone);
        dto.setLessonsTotal((int) lessonsTotal);
        if (enrollment.getEnrolledAt() != null) {
            dto.setEnrolledAt(enrollment.getEnrolledAt().toString());
        }
        return dto;
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

    private YearMonth resolvePeriod(Integer year, Integer month) {
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        int resolvedYear = year != null ? year : now.getYear();
        int resolvedMonth = month != null ? month : now.getMonthValue();
        return YearMonth.of(resolvedYear, resolvedMonth);
    }
}
