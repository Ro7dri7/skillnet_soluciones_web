package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.GammaGeneration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GammaGenerationRepository extends JpaRepository<GammaGeneration, Long> {

    Optional<GammaGeneration> findByGenerationIdAndUser_Id(String generationId, Long userId);
}
