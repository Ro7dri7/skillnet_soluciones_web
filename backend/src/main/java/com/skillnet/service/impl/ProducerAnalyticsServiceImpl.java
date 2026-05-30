package com.skillnet.service.impl;

import com.skillnet.persistence.entity.core.Course;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.entity.payments.Payment;
import com.skillnet.persistence.repository.CourseRepository;
import com.skillnet.persistence.repository.CourseReviewRepository;
import com.skillnet.persistence.repository.EnrollmentRepository;
import com.skillnet.persistence.repository.PaymentRepository;
import com.skillnet.persistence.repository.projection.CategorySalesProjection;
import com.skillnet.persistence.repository.projection.CourseRevenueProjection;
import com.skillnet.persistence.repository.projection.DailyCountProjection;
import com.skillnet.persistence.repository.projection.DailyRevenueProjection;
import com.skillnet.service.ProducerAnalyticsService;
import com.skillnet.web.dto.response.analytics.CategorySalesDTO;
import com.skillnet.web.dto.response.analytics.DailyCountDTO;
import com.skillnet.web.dto.response.analytics.DailyRevenueDTO;
import com.skillnet.web.dto.response.analytics.KpiDTO;
import com.skillnet.web.dto.response.analytics.ProducerAnalyticsDTO;
import com.skillnet.web.dto.response.analytics.RecentTransactionDTO;
import com.skillnet.web.dto.response.analytics.TopCourseDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProducerAnalyticsServiceImpl implements ProducerAnalyticsService {

    private static final String PUBLISHED_STATUS = "published";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int RECENT_TRANSACTIONS_LIMIT = 5;
    private static final int TOP_COURSES_LIMIT = 3;

    private final CourseRepository courseRepository;
    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseReviewRepository courseReviewRepository;

    public ProducerAnalyticsServiceImpl(
            CourseRepository courseRepository,
            PaymentRepository paymentRepository,
            EnrollmentRepository enrollmentRepository,
            CourseReviewRepository courseReviewRepository) {
        this.courseRepository = courseRepository;
        this.paymentRepository = paymentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseReviewRepository = courseReviewRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProducerAnalyticsDTO getAnalytics(Long professorId, Integer year, Integer month) {
        YearMonth period = resolvePeriod(year, month);
        Instant periodStart = period.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant periodEnd = period.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Course> professorCourses = courseRepository.findByProfessor_Id(professorId);
        List<Long> courseIds = professorCourses.stream().map(Course::getId).toList();

        if (courseIds.isEmpty()) {
            return emptyAnalytics(professorId);
        }

        Map<Long, Course> coursesById = professorCourses.stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));

        List<Payment> periodPayments = paymentRepository.findCompletedWithDetailsByCourseIdsAndPeriod(
                courseIds, periodStart, periodEnd);

        KpiDTO kpis = buildKpis(professorId, courseIds, periodPayments, periodStart, periodEnd);

        ProducerAnalyticsDTO analytics = new ProducerAnalyticsDTO();
        analytics.setKpis(kpis);
        analytics.setRevenueTrend(buildRevenueTrend(courseIds, periodStart, periodEnd));
        analytics.setCoursesSoldTrend(buildCoursesSoldTrend(courseIds, periodStart, periodEnd));
        analytics.setSalesByCategory(buildSalesByCategory(courseIds, periodStart, periodEnd));
        analytics.setTopCourses(buildTopCourses(
                courseIds, coursesById, periodStart, periodEnd));
        analytics.setRecentTransactions(buildRecentTransactions(periodPayments));

        return analytics;
    }

    private ProducerAnalyticsDTO emptyAnalytics(Long professorId) {
        KpiDTO kpis = new KpiDTO();
        kpis.setTotalRevenue(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        kpis.setActiveStudents(0L);
        kpis.setPublishedCourses((int) courseRepository.countByProfessor_IdAndStatus(professorId, PUBLISHED_STATUS));
        kpis.setCoursesSold(0L);
        kpis.setAvgRating(0.0);

        ProducerAnalyticsDTO analytics = new ProducerAnalyticsDTO();
        analytics.setKpis(kpis);
        analytics.setRevenueTrend(Collections.emptyList());
        analytics.setCoursesSoldTrend(Collections.emptyList());
        analytics.setSalesByCategory(Collections.emptyList());
        analytics.setTopCourses(Collections.emptyList());
        analytics.setRecentTransactions(Collections.emptyList());
        return analytics;
    }

    private KpiDTO buildKpis(
            Long professorId,
            List<Long> courseIds,
            List<Payment> periodPayments,
            Instant periodStart,
            Instant periodEnd) {

        BigDecimal totalRevenue = periodPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        long activeStudents = enrollmentRepository.countDistinctStudentsByCourseIdsAndPeriod(
                courseIds, periodStart, periodEnd);

        int publishedCourses = (int) courseRepository.countByProfessor_IdAndStatus(professorId, PUBLISHED_STATUS);

        Double avgRating = courseReviewRepository.findAverageRatingByCourseIds(courseIds);
        if (avgRating == null) {
            avgRating = 0.0;
        } else {
            avgRating = BigDecimal.valueOf(avgRating)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        KpiDTO kpis = new KpiDTO();
        kpis.setTotalRevenue(totalRevenue);
        kpis.setActiveStudents(activeStudents);
        kpis.setPublishedCourses(publishedCourses);
        kpis.setCoursesSold(paymentRepository.countCompletedSalesByCourseIds(courseIds));
        kpis.setAvgRating(avgRating);
        return kpis;
    }

    private List<DailyCountDTO> buildCoursesSoldTrend(
            List<Long> courseIds, Instant periodStart, Instant periodEnd) {
        return paymentRepository.countDailySalesByCourseIdsAndPeriod(courseIds, periodStart, periodEnd)
                .stream()
                .map(this::toDailyCountDto)
                .toList();
    }

    private DailyCountDTO toDailyCountDto(DailyCountProjection projection) {
        DailyCountDTO dto = new DailyCountDTO();
        LocalDate date = projection.getDate();
        dto.setDate(date != null ? date.format(DATE_FORMAT) : null);
        dto.setCount(projection.getCount() != null ? projection.getCount() : 0L);
        return dto;
    }

    private List<DailyRevenueDTO> buildRevenueTrend(
            List<Long> courseIds, Instant periodStart, Instant periodEnd) {
        return paymentRepository.sumDailyRevenueByCourseIdsAndPeriod(courseIds, periodStart, periodEnd)
                .stream()
                .map(this::toDailyRevenueDto)
                .toList();
    }

    private DailyRevenueDTO toDailyRevenueDto(DailyRevenueProjection projection) {
        DailyRevenueDTO dto = new DailyRevenueDTO();
        LocalDate date = projection.getDate();
        dto.setDate(date != null ? date.format(DATE_FORMAT) : null);
        dto.setAmount(scaleAmount(projection.getAmount()));
        return dto;
    }

    private List<CategorySalesDTO> buildSalesByCategory(
            List<Long> courseIds, Instant periodStart, Instant periodEnd) {
        return paymentRepository.sumSalesByCategoryForCourseIdsAndPeriod(courseIds, periodStart, periodEnd)
                .stream()
                .map(this::toCategorySalesDto)
                .toList();
    }

    private CategorySalesDTO toCategorySalesDto(CategorySalesProjection projection) {
        CategorySalesDTO dto = new CategorySalesDTO();
        dto.setCategoryName(projection.getCategoryName());
        dto.setTotalSales(scaleAmount(projection.getTotalSales()));
        return dto;
    }

    private List<TopCourseDTO> buildTopCourses(
            List<Long> courseIds,
            Map<Long, Course> coursesById,
            Instant periodStart,
            Instant periodEnd) {

        List<CourseRevenueProjection> revenueRows = paymentRepository.sumRevenueByCourseForCourseIdsAndPeriod(
                courseIds, periodStart, periodEnd);

        return revenueRows.stream()
                .limit(TOP_COURSES_LIMIT)
                .map(row -> {
                    Course course = coursesById.get(row.getCourseId());
                    TopCourseDTO dto = new TopCourseDTO();
                    dto.setId(row.getCourseId());
                    dto.setTitle(course != null ? course.getTitle() : "Curso desconocido");
                    dto.setStudentsCount(enrollmentRepository.countByCourse_Id(row.getCourseId()));
                    dto.setRevenue(scaleAmount(row.getRevenue()));
                    return dto;
                })
                .toList();
    }

    private List<RecentTransactionDTO> buildRecentTransactions(List<Payment> periodPayments) {
        return periodPayments.stream()
                .limit(RECENT_TRANSACTIONS_LIMIT)
                .map(this::toRecentTransactionDto)
                .toList();
    }

    private RecentTransactionDTO toRecentTransactionDto(Payment payment) {
        RecentTransactionDTO dto = new RecentTransactionDTO();
        dto.setDate(formatInstant(payment.getCreatedAt()));
        dto.setCourseName(payment.getCourse() != null ? payment.getCourse().getTitle() : "—");
        dto.setStudentName(resolveStudentName(payment));
        dto.setAmount(scaleAmount(payment.getAmount()));
        return dto;
    }

    private String resolveStudentName(Payment payment) {
        if (payment.getClientName() != null && !payment.getClientName().isBlank()) {
            return payment.getClientName().trim();
        }
        User user = payment.getUser();
        if (user == null) {
            return "Estudiante";
        }
        String fullName = String.join(
                        " ",
                        user.getFirstName() != null ? user.getFirstName() : "",
                        user.getLastName() != null ? user.getLastName() : "")
                .trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail() != null ? user.getEmail() : "Estudiante";
    }

    private String formatInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDate.ofInstant(instant, ZoneOffset.UTC).format(DATE_FORMAT);
    }

    private BigDecimal scaleAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private YearMonth resolvePeriod(Integer year, Integer month) {
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        int resolvedYear = year != null ? year : now.getYear();
        int resolvedMonth = month != null ? month : now.getMonthValue();
        return YearMonth.of(resolvedYear, resolvedMonth);
    }
}
