package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.PodcastGeneration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PodcastGenerationRepository extends JpaRepository<PodcastGeneration, Long> {

    Optional<PodcastGeneration> findByIdAndRequestedBy_Id(Long id, Long userId);
}
