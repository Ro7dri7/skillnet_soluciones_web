package com.skillnet.persistence.repository;

import com.skillnet.persistence.entity.core.InfoproductorServiceOffering;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InfoproductorServiceOfferingRepository extends JpaRepository<InfoproductorServiceOffering, Long> {

    List<InfoproductorServiceOffering> findByActiveTrueOrderBySectionAscSortOrderAscIdAsc();

    Optional<InfoproductorServiceOffering> findByIdAndActiveTrue(Long id);

    boolean existsByCapabilityKey(String capabilityKey);

    Optional<InfoproductorServiceOffering> findByCapabilityKey(String capabilityKey);
}
