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
}
