package com.skillnet.service;

import com.skillnet.persistence.entity.core.User;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AuthRoleService {

    /**
     * Modelo Hotmart (Lernymart {@code switch_role}): cualquier usuario activo puede alternar
     * entre estudiante e infoproductor; solo admin requiere permisos explícitos.
     */
    public boolean canAssumeRole(User user, String requestedRole) {
        if (user == null || requestedRole == null || requestedRole.isBlank() || !user.isActive()) {
            return false;
        }
        String role = requestedRole.trim().toLowerCase(Locale.ROOT);
        return switch (role) {
            case "admin" -> user.isSuperUser() || user.isStaff() || "admin".equalsIgnoreCase(user.getRole());
            case "infoproductor", "student" -> !"admin".equalsIgnoreCase(user.getRole()) || user.isSuperUser();
            default -> false;
        };
    }
}
