package com.skillnet.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillnet.persistence.entity.core.AuditLog;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.AuditLogRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.AuditService;
import com.skillnet.service.audit.AuditLogFilter;
import com.skillnet.util.ClientIpResolver;
import com.skillnet.web.dto.response.AuditLogResponseDTO;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final int EXPORT_LIMIT = 10_000;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Lima");
    private static final DateTimeFormatter CSV_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(BUSINESS_ZONE);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void logAction(String action, String entityName, Long entityId, String userEmail, String details) {
        String resolvedEmail = userEmail != null && !userEmail.isBlank() ? userEmail : resolveCurrentUserEmail();
        User actor = resolveUser(resolvedEmail);

        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setModelName(entityName);
        if (entityId != null) {
            entry.setObjectId(Math.toIntExact(entityId));
        }
        entry.setUser(actor);
        entry.setObjectRepr(buildObjectRepr(entityName, entityId, details));
        entry.setChanges(buildChanges(details, resolvedEmail));
        entry.setIpAddress(clientIpResolver.resolveCurrentRequestIp());
        entry.setUserAgent(clientIpResolver.resolveCurrentUserAgent());
        entry.setTimestamp(Instant.now());

        auditLogRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> listAuditLogs(AuditLogFilter filter, Pageable pageable) {
        return auditLogRepository
                .findFiltered(
                        filter.normalizedEmail(),
                        filter.normalizedAction(),
                        filter.getStartDate(),
                        filter.getEndDate(),
                        pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToCsv(AuditLogFilter filter) {
        List<AuditLog> rows = auditLogRepository.findFilteredForExport(
                filter.normalizedEmail(),
                filter.normalizedAction(),
                filter.getStartDate(),
                filter.getEndDate(),
                EXPORT_LIMIT);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
            writer.println("ID,Timestamp,User,DisplayName,Action,Entity,EntityID,IP,UserAgent,Details");
            for (AuditLog entry : rows) {
                AuditLogResponseDTO dto = toResponse(entry);
                writer.printf(
                        "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        csvValue(dto.getId()),
                        csvValue(formatTimestamp(dto.getTimestamp())),
                        csvValue(dto.getUserEmail()),
                        csvValue(dto.getUserDisplayName()),
                        csvValue(dto.getAction()),
                        csvValue(dto.getEntityName()),
                        csvValue(dto.getEntityId()),
                        csvValue(dto.getIpAddress()),
                        csvValue(dto.getUserAgent()),
                        csvValue(dto.getDetails()));
            }
        }
        return output.toByteArray();
    }

    private User resolveUser(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    private String resolveCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getEmail();
        }
        return authentication.getName();
    }

    private String buildObjectRepr(String entityName, Long entityId, String details) {
        String base = entityName + (entityId != null ? " #" + entityId : "");
        if (details != null && !details.isBlank()) {
            String combined = base + " — " + details;
            return combined.length() > 200 ? combined.substring(0, 197) + "..." : combined;
        }
        return base.length() > 200 ? base.substring(0, 200) : base;
    }

    private JsonNode buildChanges(String details, String userEmail) {
        var node = objectMapper.createObjectNode();
        if (details != null && !details.isBlank()) {
            node.put("detail", details);
        }
        if (userEmail != null && !userEmail.isBlank()) {
            node.put("userEmail", userEmail);
        }
        return node.isEmpty() ? null : node;
    }

    private AuditLogResponseDTO toResponse(AuditLog entry) {
        String userEmail = entry.getUser() != null ? entry.getUser().getEmail() : null;
        if ((userEmail == null || userEmail.isBlank()) && entry.getChanges() != null) {
            JsonNode emailNode = entry.getChanges().get("userEmail");
            if (emailNode != null && emailNode.isTextual()) {
                userEmail = emailNode.asText();
            }
        }

        String details = null;
        if (entry.getChanges() != null && entry.getChanges().has("detail")) {
            details = entry.getChanges().get("detail").asText();
        } else if (entry.getObjectRepr() != null) {
            details = entry.getObjectRepr();
        }

        Long entityId = entry.getObjectId() != null ? entry.getObjectId().longValue() : null;

        return AuditLogResponseDTO.builder()
                .id(entry.getId())
                .action(entry.getAction())
                .entityName(entry.getModelName())
                .entityId(entityId)
                .userEmail(userEmail)
                .userDisplayName(formatDisplayName(entry.getUser()))
                .ipAddress(entry.getIpAddress())
                .userAgent(entry.getUserAgent())
                .details(details)
                .timestamp(entry.getTimestamp())
                .build();
    }

    private String formatDisplayName(User user) {
        if (user == null) {
            return null;
        }
        String fullName = String.join(
                        " ",
                        user.getFirstName() != null ? user.getFirstName().trim() : "",
                        user.getLastName() != null ? user.getLastName().trim() : "")
                .trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail();
    }

    private String formatTimestamp(Instant timestamp) {
        return timestamp == null ? "" : CSV_TIMESTAMP.format(timestamp);
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
