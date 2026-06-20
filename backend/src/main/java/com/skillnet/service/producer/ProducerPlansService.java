package com.skillnet.service.producer;

import com.skillnet.persistence.entity.core.InfoproductorServiceOffering;
import com.skillnet.persistence.repository.InfoproductorServiceOfferingRepository;
import com.skillnet.service.entitlement.ServiceEntitlementService;
import com.skillnet.web.dto.response.ProducerCapabilityStatusDTO;
import com.skillnet.web.dto.response.ServiceEntitlementResponseDTO;
import com.skillnet.web.dto.response.ServiceOfferingResponseDTO;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProducerPlansService {

    private final InfoproductorServiceOfferingRepository serviceOfferingRepository;
    private final ServiceEntitlementService entitlementService;

    @Transactional(readOnly = true)
    public List<ServiceOfferingResponseDTO> listActiveOfferings() {
        return serviceOfferingRepository.findByActiveTrueOrderBySectionAscSortOrderAscIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceEntitlementResponseDTO> listEntitlements(Long userId) {
        return entitlementService.listForUser(userId);
    }

    @Transactional(readOnly = true)
    public Map<String, ProducerCapabilityStatusDTO> capabilities(Long userId) {
        return entitlementService.capabilitySummary(userId);
    }

    private ServiceOfferingResponseDTO toResponse(InfoproductorServiceOffering offering) {
        ServiceOfferingResponseDTO dto = new ServiceOfferingResponseDTO();
        dto.setId(offering.getId());
        dto.setSection(offering.getSection());
        dto.setTitle(offering.getTitle());
        dto.setDescription(offering.getDescription());
        dto.setPriceUsd(offering.getPriceUsd());
        dto.setIconClass(offering.getIconClass());
        dto.setSortOrder(offering.getSortOrder());
        dto.setActive(offering.isActive());
        dto.setFeatures(offering.getFeatures());
        dto.setOriginalPriceUsd(offering.getOriginalPriceUsd());
        dto.setSaveAmountUsd(offering.getSaveAmountUsd());
        dto.setFeatured(offering.isFeatured());
        dto.setCapabilityKey(offering.getCapabilityKey());
        dto.setIncludedUses(offering.getIncludedUses());
        dto.setCreatedAt(offering.getCreatedAt());
        dto.setUpdatedAt(offering.getUpdatedAt());
        return dto;
    }
}
