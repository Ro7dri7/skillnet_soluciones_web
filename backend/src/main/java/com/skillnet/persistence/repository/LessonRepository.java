package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.Lesson;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourse_Id(Long courseId);

    List<Lesson> findByCourse_IdOrderByOrderIndexAsc(Long courseId);

    List<Lesson> findByCourse_IdAndStatus(Long courseId, String status);

    Optional<Lesson> findByIdAndCourse_Id(Long id, Long courseId);

    List<Lesson> findBySection_Id(Long sectionId);

    long countByCourse_Id(Long courseId);
}
