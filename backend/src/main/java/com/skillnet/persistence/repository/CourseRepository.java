package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.Course;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findBySlug(String slug);

    List<Course> findByStatus(String status);

    List<Course> findByProfessor_Id(Long professorId);

    List<Course> findAllByProfessorIdOrderByCreatedAtDesc(Long professorId);

    boolean existsByProfessor_Id(Long professorId);

    long countByProfessor_IdAndStatus(Long professorId, String status);

    List<Course> findByCategoryIgnoreCase(String category);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant start, Instant end);

    long countByStatus(String status);
}
