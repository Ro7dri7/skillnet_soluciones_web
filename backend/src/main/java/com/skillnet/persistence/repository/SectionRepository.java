package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.Section;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByCourse_IdOrderByOrderIndexAsc(Long courseId);
}
