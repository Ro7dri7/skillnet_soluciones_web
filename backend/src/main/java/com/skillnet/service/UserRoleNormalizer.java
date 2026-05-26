package com.skillnet.service;

import com.skillnet.persistence.entity.core.User;
import java.util.Locale;

/**
 * Modelo Hotmart / Lernymart: al registrarse como estudiante o infoproductor, la cuenta
 * habilita ambas capacidades; {@code active_role} indica la vista activa.
 */
public final class UserRoleNormalizer {

    private UserRoleNormalizer() {}

    public static void applyDualRoleCapabilities(User user, String primaryRole) {
        String role = normalizePrimaryRole(primaryRole);
        user.setRole(role);
        user.setActiveRole(role);
        if ("admin".equals(role)) {
            return;
        }
        user.setStudent(true);
        user.setInfoproductor(true);
    }

    /**
     * Usuarios legacy con un solo flag: activa estudiante + infoproductor (excepto admin).
     */
    public static void ensureDualCapabilities(User user) {
        if (user == null || isAdminAccount(user)) {
            return;
        }
        user.setStudent(true);
        user.setInfoproductor(true);
    }

    public static void applyActiveRoleSwitch(User user, String requestedRole) {
        String role = normalizePrimaryRole(requestedRole);
        if ("admin".equals(role) && !isAdminAccount(user)) {
            throw new IllegalArgumentException("Admin role not allowed");
        }
        user.setActiveRole(role);
        if ("admin".equals(role)) {
            return;
        }
        user.setStudent(true);
        if ("infoproductor".equals(role)) {
            user.setInfoproductor(true);
        }
    }

    private static String normalizePrimaryRole(String role) {
        if (role == null || role.isBlank()) {
            return "student";
        }
        return role.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isAdminAccount(User user) {
        return user.isSuperUser()
                || user.isStaff()
                || "admin".equalsIgnoreCase(user.getRole());
    }
}
