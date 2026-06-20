package com.skillnet.service.entitlement;

import com.skillnet.persistence.entity.core.InfoproductorServiceOffering;
import com.skillnet.persistence.entity.core.ServiceEntitlement;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.entity.payments.Payment;
import com.skillnet.persistence.repository.ServiceEntitlementRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.web.dto.response.ProducerCapabilityStatusDTO;
import com.skillnet.web.dto.response.ServiceEntitlementResponseDTO;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ServiceEntitlementService {

    public static final String CAPABILITY_GAMMA_EBOOK = "gamma_ebook";
    public static final String CAPABILITY_PODCAST_AI = "podcast_ai";

    /** Cuota inicial por infoproductor (sin compra de plan). */
    public static final int STARTER_GAMMA_EBOOK_USES = 20;
    public static final int STARTER_PODCAST_AI_USES = 5;

    private static final List<String> TRACKED_CAPABILITIES =
            List.of(CAPABILITY_GAMMA_EBOOK, CAPABILITY_PODCAST_AI);

    private final ServiceEntitlementRepository entitlementRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean hasActiveCapability(Long userId, String capabilityKey) {
        if (isAdminBypass(userId)) {
            return true;
        }
        return entitlementRepository.existsByUser_IdAndStatusAndUsesRemainingGreaterThanAndOffering_CapabilityKey(
                userId, "ACTIVE", 0, capabilityKey);
    }

    @Transactional(readOnly = true)
    public Map<String, ProducerCapabilityStatusDTO> capabilitySummary(Long userId) {
        Map<String, ProducerCapabilityStatusDTO> summary = new LinkedHashMap<>();
        boolean admin = isAdminBypass(userId);
        for (String key : TRACKED_CAPABILITIES) {
            if (admin) {
                summary.put(key, ProducerCapabilityStatusDTO.builder()
                        .capabilityKey(key)
                        .active(true)
                        .usesRemaining(999)
                        .build());
                continue;
            }
            int uses = entitlementRepository
                    .findByUser_IdOrderByCreatedAtDesc(userId)
                    .stream()
                    .filter(e -> "ACTIVE".equals(e.getStatus()))
                    .filter(e -> e.getUsesRemaining() > 0)
                    .filter(e -> key.equals(e.getOffering().getCapabilityKey()))
                    .mapToInt(ServiceEntitlement::getUsesRemaining)
                    .sum();
            summary.put(
                    key,
                    ProducerCapabilityStatusDTO.builder()
                            .capabilityKey(key)
                            .active(uses > 0)
                            .usesRemaining(uses)
                            .build());
        }
        return summary;
    }

    @Transactional(readOnly = true)
    public List<ServiceEntitlementResponseDTO> listForUser(Long userId) {
        return entitlementRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void consumeUse(Long userId, String capabilityKey) {
        if (isAdminBypass(userId)) {
            return;
        }
        ServiceEntitlement entitlement = entitlementRepository
                .findFirstActiveForCapability(userId, capabilityKey)
                .orElseThrow(() -> entitlementRequiredException(capabilityKey));

        entitlement.setUsesRemaining(entitlement.getUsesRemaining() - 1);
        if (entitlement.getUsesRemaining() <= 0) {
            entitlement.setStatus("EXPIRED");
        }
        entitlement.setUpdatedAt(Instant.now());
        entitlementRepository.save(entitlement);
    }

    @Transactional
    public void grantStarterEntitlement(
            User user, InfoproductorServiceOffering offering, int uses) {
        if (user == null || offering == null) {
            return;
        }
        String capabilityKey = offering.getCapabilityKey();
        if (capabilityKey == null || capabilityKey.isBlank()) {
            return;
        }
        if (entitlementRepository.existsByUser_IdAndStatusAndUsesRemainingGreaterThanAndOffering_CapabilityKey(
                user.getId(), "ACTIVE", 0, capabilityKey)) {
            return;
        }
        int grantedUses = Math.max(1, uses);
        Instant now = Instant.now();
        ServiceEntitlement entitlement = new ServiceEntitlement();
        entitlement.setUser(user);
        entitlement.setOffering(offering);
        entitlement.setPayment(null);
        entitlement.setStatus("ACTIVE");
        entitlement.setUsesRemaining(grantedUses);
        entitlement.setCreatedAt(now);
        entitlement.setUpdatedAt(now);
        entitlementRepository.save(entitlement);
    }

    /** Asegura cuota mínima en entitlements activos (p. ej. tras cambiar el límite global). */
    @Transactional
    public void bumpActiveQuotaMinimum(Long userId, String capabilityKey, int minimumUses) {
        if (userId == null || minimumUses < 1) {
            return;
        }
        entitlementRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .filter(e -> "ACTIVE".equals(e.getStatus()))
                .filter(e -> e.getOffering() != null && capabilityKey.equals(e.getOffering().getCapabilityKey()))
                .forEach(entitlement -> {
                    if (entitlement.getUsesRemaining() < minimumUses) {
                        entitlement.setUsesRemaining(minimumUses);
                        entitlement.setUpdatedAt(Instant.now());
                        entitlementRepository.save(entitlement);
                    }
                });
    }

    @Transactional
    public void grantFromPayment(Payment payment) {
        InfoproductorServiceOffering offering = payment.getServiceOffering();
        if (offering == null) {
            return;
        }
        if (entitlementRepository.findByPayment_Id(payment.getId()).isPresent()) {
            return;
        }
        int uses = Math.max(1, offering.getIncludedUses());
        Instant now = Instant.now();
        ServiceEntitlement entitlement = new ServiceEntitlement();
        entitlement.setUser(payment.getUser());
        entitlement.setOffering(offering);
        entitlement.setPayment(payment);
        entitlement.setStatus("ACTIVE");
        entitlement.setUsesRemaining(uses);
        entitlement.setCreatedAt(now);
        entitlement.setUpdatedAt(now);
        entitlementRepository.save(entitlement);
    }

    private boolean isAdminBypass(Long userId) {
        return userRepository
                .findById(userId)
                .map(u -> u.isSuperUser() || u.isStaff() || "admin".equalsIgnoreCase(u.getRole()))
                .orElse(false);
    }

    private ResponseStatusException entitlementRequiredException(String capabilityKey) {
        String feature = CAPABILITY_PODCAST_AI.equals(capabilityKey) ? "podcasts con IA" : "ebooks con IA (Gamma)";
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Necesitas un plan activo con acceso a "
                        + feature
                        + ". Adquiérelo en Planes para infoproductores.");
    }

    private ServiceEntitlementResponseDTO toResponse(ServiceEntitlement entitlement) {
        InfoproductorServiceOffering offering = entitlement.getOffering();
        return ServiceEntitlementResponseDTO.builder()
                .id(entitlement.getId())
                .status(entitlement.getStatus())
                .usesRemaining(entitlement.getUsesRemaining())
                .capabilityKey(offering != null ? offering.getCapabilityKey() : null)
                .offeringTitle(offering != null ? offering.getTitle() : null)
                .createdAt(entitlement.getCreatedAt())
                .build();
    }
}
