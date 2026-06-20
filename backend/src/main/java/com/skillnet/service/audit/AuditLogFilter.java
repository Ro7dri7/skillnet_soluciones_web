package com.skillnet.service.audit;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuditLogFilter {

    String email;
    String action;
    Instant startDate;
    Instant endDate;

    public String normalizedEmail() {
        return email == null || email.isBlank() ? null : email.trim();
    }

    public String normalizedAction() {
        return action == null || action.isBlank() ? null : action.trim();
    }
}
