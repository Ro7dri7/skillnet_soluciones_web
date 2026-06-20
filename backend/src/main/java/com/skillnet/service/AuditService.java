package com.skillnet.service;

import com.skillnet.service.audit.AuditLogFilter;
import com.skillnet.web.dto.response.AuditLogResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {

    void logAction(String action, String entityName, Long entityId, String userEmail, String details);

    Page<AuditLogResponseDTO> listAuditLogs(AuditLogFilter filter, Pageable pageable);

    byte[] exportToCsv(AuditLogFilter filter);
}
