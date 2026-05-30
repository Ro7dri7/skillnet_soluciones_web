package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.payments.Payment;
import com.skillnet.persistence.repository.projection.CategorySalesProjection;
import com.skillnet.persistence.repository.projection.CourseRevenueProjection;
import com.skillnet.persistence.repository.projection.DailyCountProjection;
import com.skillnet.persistence.repository.projection.DailyRevenueProjection;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    String COMPLETED_STATUS = "COMPLETED";
    String SUCCEEDED_STATUS = "SUCCEEDED";

    List<Payment> findByUser_Id(Long userId);

    List<Payment> findByUser_IdOrderByCreatedAtDesc(Long userId);

    List<Payment> findByStatus(String status);

    List<Payment> findByUser_IdAndStatus(Long userId, String status);

    List<Payment> findByCourse_Id(Long courseId);

    Optional<Payment> findByMercadopagoPaymentId(String mercadopagoPaymentId);

    Optional<Payment> findByDlocalPaymentId(String dlocalPaymentId);

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.course c
            JOIN FETCH p.user u
            WHERE p.course.id IN :courseIds
              AND p.status IN ('COMPLETED', 'SUCCEEDED')
              AND p.createdAt >= :start
              AND p.createdAt < :end
            ORDER BY p.createdAt DESC
            """)
    List<Payment> findCompletedWithDetailsByCourseIdsAndPeriod(
            @Param("courseIds") Collection<Long> courseIds,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT CAST(p.createdAt AS localdate) AS date, SUM(p.amount) AS amount
            FROM Payment p
            WHERE p.course.id IN :courseIds
              AND p.status IN ('COMPLETED', 'SUCCEEDED')
              AND p.createdAt >= :start
              AND p.createdAt < :end
            GROUP BY CAST(p.createdAt AS localdate)
            ORDER BY CAST(p.createdAt AS localdate)
            """)
    List<DailyRevenueProjection> sumDailyRevenueByCourseIdsAndPeriod(
            @Param("courseIds") Collection<Long> courseIds,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT COALESCE(c.category, 'Sin categoría') AS categoryName, SUM(p.amount) AS totalSales
            FROM Payment p
            JOIN p.course c
            WHERE p.course.id IN :courseIds
              AND p.status IN ('COMPLETED', 'SUCCEEDED')
              AND p.createdAt >= :start
              AND p.createdAt < :end
            GROUP BY COALESCE(c.category, 'Sin categoría')
            ORDER BY SUM(p.amount) DESC
            """)
    List<CategorySalesProjection> sumSalesByCategoryForCourseIdsAndPeriod(
            @Param("courseIds") Collection<Long> courseIds,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT p.course.id AS courseId, SUM(p.amount) AS revenue
            FROM Payment p
            WHERE p.course.id IN :courseIds
              AND p.status IN ('COMPLETED', 'SUCCEEDED')
              AND p.createdAt >= :start
              AND p.createdAt < :end
            GROUP BY p.course.id
            ORDER BY SUM(p.amount) DESC
            """)
    List<CourseRevenueProjection> sumRevenueByCourseForCourseIdsAndPeriod(
            @Param("courseIds") Collection<Long> courseIds,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT CAST(p.createdAt AS localdate) AS date, COUNT(p) AS count
            FROM Payment p
            WHERE p.course.id IN :courseIds
              AND p.status IN ('COMPLETED', 'SUCCEEDED')
              AND p.createdAt >= :start
              AND p.createdAt < :end
            GROUP BY CAST(p.createdAt AS localdate)
            ORDER BY CAST(p.createdAt AS localdate)
            """)
    List<DailyCountProjection> countDailySalesByCourseIdsAndPeriod(
            @Param("courseIds") Collection<Long> courseIds,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
            SELECT COUNT(p)
            FROM Payment p
            WHERE p.course.id IN :courseIds
              AND p.status IN ('COMPLETED', 'SUCCEEDED')
            """)
    long countCompletedSalesByCourseIds(@Param("courseIds") Collection<Long> courseIds);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.status IN ('COMPLETED', 'SUCCEEDED')
              AND p.createdAt >= :start
              AND p.createdAt < :end
            """)
    java.math.BigDecimal sumCompletedAmountBetween(
            @Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            SELECT COUNT(p)
            FROM Payment p
            WHERE p.status IN ('COMPLETED', 'SUCCEEDED')
              AND p.createdAt >= :start
              AND p.createdAt < :end
            """)
    long countCompletedBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            SELECT CAST(p.createdAt AS localdate) AS date, SUM(p.amount) AS amount
            FROM Payment p
            WHERE p.status IN ('COMPLETED', 'SUCCEEDED')
              AND p.createdAt >= :start
              AND p.createdAt < :end
            GROUP BY CAST(p.createdAt AS localdate)
            ORDER BY CAST(p.createdAt AS localdate)
            """)
    List<DailyRevenueProjection> sumDailyRevenuePlatform(
            @Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            SELECT p FROM Payment p
            LEFT JOIN FETCH p.course c
            LEFT JOIN FETCH p.user u
            WHERE p.status IN ('COMPLETED', 'SUCCEEDED', 'PENDING', 'FAILED')
            ORDER BY p.createdAt DESC
            """)
    List<Payment> findRecentWithDetails(Pageable pageable);

    @Query("""
            SELECT p.course.professor.id, SUM(p.amount)
            FROM Payment p
            WHERE p.status IN ('COMPLETED', 'SUCCEEDED')
              AND p.course IS NOT NULL
              AND p.course.professor IS NOT NULL
              AND p.createdAt >= :start
              AND p.createdAt < :end
            GROUP BY p.course.professor.id
            ORDER BY SUM(p.amount) DESC
            """)
    List<Object[]> sumRevenueGroupedByProfessor(@Param("start") Instant start, @Param("end") Instant end);
}
