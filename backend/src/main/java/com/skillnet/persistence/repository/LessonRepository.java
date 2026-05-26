package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.Lesson;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("SELECT l FROM Lesson l JOIN FETCH l.course WHERE l.id = :id")
    Optional<Lesson> findByIdWithCourse(@Param("id") Long id);

    List<Lesson> findByCourse_Id(Long courseId);

    List<Lesson> findByCourse_IdOrderByOrderIndexAsc(Long courseId);

    List<Lesson> findByCourse_IdAndStatus(Long courseId, String status);

    Optional<Lesson> findByIdAndCourse_Id(Long id, Long courseId);

    List<Lesson> findBySection_Id(Long sectionId);

    List<Lesson> findBySection_IdOrderByOrderIndexAsc(Long sectionId);

    long countByCourse_Id(Long courseId);
}
