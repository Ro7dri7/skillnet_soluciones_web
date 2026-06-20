package com.skillnet.service.admin;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.skillnet.persistence.entity.core.InfoproductorServiceOffering;
import com.skillnet.persistence.repository.InfoproductorServiceOfferingRepository;
import com.skillnet.web.dto.request.ServiceOfferingRequestDTO;
import com.skillnet.web.dto.response.ServiceOfferingResponseDTO;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminServiceOfferingService {

    private final InfoproductorServiceOfferingRepository serviceOfferingRepository;

    @Transactional(readOnly = true)
    public List<ServiceOfferingResponseDTO> listAll() {
        return serviceOfferingRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceOfferingResponseDTO getById(Long id) {
        return serviceOfferingRepository
                .findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta no encontrada"));
    }

    @Transactional
    public ServiceOfferingResponseDTO create(ServiceOfferingRequestDTO dto) {
        InfoproductorServiceOffering offering = new InfoproductorServiceOffering();
        applyDto(offering, dto);
        Instant now = Instant.now();
        offering.setCreatedAt(now);
        offering.setUpdatedAt(now);
        return toResponse(serviceOfferingRepository.save(offering));
    }

    @Transactional
    public ServiceOfferingResponseDTO update(Long id, ServiceOfferingRequestDTO dto) {
        InfoproductorServiceOffering offering = serviceOfferingRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta no encontrada"));
        applyDto(offering, dto);
        offering.setUpdatedAt(Instant.now());
        return toResponse(serviceOfferingRepository.save(offering));
    }

    @Transactional
    public void delete(Long id) {
        if (!serviceOfferingRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Oferta no encontrada");
        }
        serviceOfferingRepository.deleteById(id);
    }

    private void applyDto(InfoproductorServiceOffering offering, ServiceOfferingRequestDTO dto) {
        offering.setSection(dto.getSection());
        offering.setTitle(dto.getTitle());
        offering.setDescription(dto.getDescription());
        offering.setPriceUsd(dto.getPriceUsd());
        offering.setIconClass(dto.getIconClass());
        offering.setSortOrder(dto.getSortOrder());
        offering.setActive(dto.isActive());
        offering.setFeatures(dto.getFeatures() != null ? dto.getFeatures() : JsonNodeFactory.instance.arrayNode());
        offering.setOriginalPriceUsd(dto.getOriginalPriceUsd());
        offering.setSaveAmountUsd(dto.getSaveAmountUsd());
        offering.setFeatured(dto.isFeatured());
        offering.setCapabilityKey(dto.getCapabilityKey() != null ? dto.getCapabilityKey() : "");
        offering.setIncludedUses(dto.getIncludedUses());
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
