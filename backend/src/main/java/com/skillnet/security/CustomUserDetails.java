package com.skillnet.security;

import com.skillnet.persistence.entity.core.User;
import java.util.Collection;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final Long id;
    private final String email;
    private final String password;
    /** Rol efectivo de la sesión (claim JWT / active_role). */
    private final String role;
    private final boolean active;

    public CustomUserDetails(User user) {
        this(user, RoleAuthorityResolver.defaultActiveRole(user));
    }

    public CustomUserDetails(User user, String effectiveRole) {
        this.user = user;
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = effectiveRole;
        this.active = user.isActive();
    }

    public CustomUserDetails withEffectiveRole(String requestedRole) {
        if (requestedRole == null
                || requestedRole.isBlank()
                || requestedRole.equalsIgnoreCase(this.role)) {
            return this;
        }
        if (!RoleAuthorityResolver.canAssumeRole(user, requestedRole)) {
            return this;
        }
        return new CustomUserDetails(user, requestedRole.trim().toLowerCase());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return RoleAuthorityResolver.resolveAuthorities(user);
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
