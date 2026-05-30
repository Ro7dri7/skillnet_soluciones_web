package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.Section;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByCourse_IdOrderByOrderIndexAsc(Long courseId);

    long countByCourse_Id(Long courseId);

    @Query("SELECT s FROM Section s JOIN FETCH s.course WHERE s.id = :id")
    Optional<Section> findByIdWithCourse(@Param("id") Long id);
}
