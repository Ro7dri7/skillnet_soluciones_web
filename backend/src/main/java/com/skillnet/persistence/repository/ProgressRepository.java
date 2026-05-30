package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.Progress;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgressRepository extends JpaRepository<Progress, Long> {

    Optional<Progress> findByUser_IdAndLesson_Id(Long userId, Long lessonId);

    @Query("""
            SELECT p.lesson.id
            FROM Progress p
            WHERE p.user.id = :userId
              AND p.lesson.course.id = :courseId
              AND p.completed = true
            """)
    Set<Long> findCompletedLessonIdsByUserAndCourse(
            @Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("""
            SELECT COUNT(p)
            FROM Progress p
            WHERE p.user.id = :userId
              AND p.lesson.course.id = :courseId
              AND p.completed = true
            """)
    long countCompletedByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    List<Progress> findByUser_IdAndLesson_Course_Id(Long userId, Long courseId);
}
