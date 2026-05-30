package com.skillnet.security;

import com.skillnet.persistence.entity.core.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class RoleAuthorityResolver {

    private RoleAuthorityResolver() {}

    /**
     * Modelo Lernymart/Hotmart: cuentas estándar llevan STUDENT + INFOPRODUCTOR en Spring Security
     * para que APIs de productor no devuelvan 403 al alternar vista. El claim {@code role} del JWT
     * sigue indicando la vista activa (student / infoproductor).
     */
    public static Collection<SimpleGrantedAuthority> resolveAuthorities(User user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (user != null && isAdminAccount(user)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
            authorities.add(new SimpleGrantedAuthority("ROLE_INFOPRODUCTOR"));
            return authorities;
        }
        // Hotmart / Lernymart: toda cuenta estándar puede usar APIs de estudiante e infoproductor.
        authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
        authorities.add(new SimpleGrantedAuthority("ROLE_INFOPRODUCTOR"));
        return authorities;
    }

    public static boolean isAdminAccount(User user) {
        return user != null
                && (user.isSuperUser() || user.isStaff() || "admin".equalsIgnoreCase(user.getRole()));
    }

    public static String defaultActiveRole(User user) {
        if (user.getActiveRole() != null && !user.getActiveRole().isBlank()) {
            return user.getActiveRole().trim().toLowerCase(Locale.ROOT);
        }
        if (user.getRole() != null && !user.getRole().isBlank()) {
            return user.getRole().trim().toLowerCase(Locale.ROOT);
        }
        return "student";
    }

    public static boolean canAssumeRole(User user, String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            return false;
        }
        String role = requestedRole.trim().toLowerCase(Locale.ROOT);
        if (!user.isActive()) {
            return false;
        }
        return switch (role) {
            case "admin" -> user.isSuperUser() || user.isStaff() || "admin".equalsIgnoreCase(user.getRole());
            case "infoproductor", "student" -> !"admin".equalsIgnoreCase(user.getRole()) || user.isSuperUser();
            default -> false;
        };
    }

    private static boolean equalsIgnoreCase(String value, String expected) {
        return value != null && expected.equalsIgnoreCase(value.trim());
    }

    public static String toAuthority(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_STUDENT";
        }
        return "ROLE_" + role.trim().toUpperCase(Locale.ROOT);
    }
}
