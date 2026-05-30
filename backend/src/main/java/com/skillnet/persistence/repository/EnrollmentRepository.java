package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.Enrollment;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByUser_IdAndCourse_Id(Long userId, Long courseId);

    List<Enrollment> findByUser_Id(Long userId);

    @Query("""
            SELECT e FROM Enrollment e
            JOIN FETCH e.course c
            LEFT JOIN FETCH c.professor
            WHERE e.user.id = :userId
            ORDER BY e.enrolledAt DESC
            """)
    List<Enrollment> findByUser_IdWithCourseOrderByEnrolledAtDesc(@Param("userId") Long userId);

    List<Enrollment> findByCourse_Id(Long courseId);

    boolean existsByUser_IdAndCourse_Id(Long userId, Long courseId);

    long countByCourse_Id(Long courseId);

    @Query("""
            SELECT COUNT(DISTINCT e.user.id)
            FROM Enrollment e
            WHERE e.course.id IN :courseIds
              AND e.enrolledAt >= :start
              AND e.enrolledAt < :end
            """)
    long countDistinctStudentsByCourseIdsAndPeriod(
            @Param("courseIds") Collection<Long> courseIds,
            @Param("start") Instant start,
            @Param("end") Instant end);

    long countByEnrolledAtGreaterThanEqualAndEnrolledAtLessThan(Instant start, Instant end);

    long countByCompletedTrueAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(Instant start, Instant end);

    long countByUser_Id(Long userId);

    @Query("""
            SELECT COUNT(e)
            FROM Enrollment e
            WHERE e.user.id = :userId
              AND e.enrolledAt >= :start
              AND e.enrolledAt < :end
            """)
    long countByUser_IdAndEnrolledAtBetween(
            @Param("userId") Long userId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            SELECT COUNT(e)
            FROM Enrollment e
            WHERE e.user.id = :userId
              AND e.completed = true
            """)
    long countCompletedAllTimeByUser_Id(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(e)
            FROM Enrollment e
            WHERE e.user.id = :userId
              AND e.completed = true
              AND e.completedAt >= :start
              AND e.completedAt < :end
            """)
    long countCompletedByUser_IdAndPeriod(
            @Param("userId") Long userId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            SELECT COUNT(e)
            FROM Enrollment e
            WHERE e.user.id = :userId
              AND e.completed = false
            """)
    long countActiveByUser_Id(@Param("userId") Long userId);

    @Query("""
            SELECT CAST(e.enrolledAt AS localdate) AS date, COUNT(e) AS count
            FROM Enrollment e
            WHERE e.user.id = :userId
              AND e.enrolledAt >= :start
              AND e.enrolledAt < :end
            GROUP BY CAST(e.enrolledAt AS localdate)
            ORDER BY CAST(e.enrolledAt AS localdate)
            """)
    List<com.skillnet.persistence.repository.projection.DailyCountProjection> countDailyEnrollmentsByUserAndPeriod(
            @Param("userId") Long userId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            SELECT COALESCE(c.category, 'General') AS categoryName,
                   AVG(CASE WHEN e.completed = true THEN 100.0 ELSE 0.0 END) AS percent
            FROM Enrollment e
            JOIN e.course c
            WHERE e.user.id = :userId
              AND e.enrolledAt >= :start
              AND e.enrolledAt < :end
            GROUP BY COALESCE(c.category, 'General')
            ORDER BY COALESCE(c.category, 'General')
            """)
    List<com.skillnet.persistence.repository.projection.CategoryProgressProjection> progressByCategoryForUserAndPeriod(
            @Param("userId") Long userId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            SELECT e FROM Enrollment e
            JOIN FETCH e.course c
            LEFT JOIN FETCH c.professor
            WHERE e.user.id = :userId
            ORDER BY e.enrolledAt DESC
            """)
    List<Enrollment> findAllByUser_IdWithCourse(@Param("userId") Long userId);
}
