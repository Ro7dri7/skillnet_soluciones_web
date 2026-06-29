package com.skillnet.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.skillnet.mapper.UserMapper;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.security.CustomUserDetails;
import com.skillnet.security.JwtService;
import com.skillnet.security.RoleAuthorityResolver;
import com.skillnet.service.UserRoleNormalizer;
import com.skillnet.service.auth.EmailVerificationService;
import com.skillnet.web.dto.request.GoogleLoginRequestDTO;
import com.skillnet.web.dto.response.AuthResponseDTO;
import com.skillnet.web.dto.response.UserSummaryDTO;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailVerificationService emailVerificationService;
    private final String googleClientId;

    public GoogleAuthService(
            UserRepository userRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            EmailVerificationService emailVerificationService,
            @Value("${skillnet.google.client-id}") String googleClientId) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.emailVerificationService = emailVerificationService;
        this.googleClientId = googleClientId;
    }

    @Transactional
    public AuthResponseDTO authenticate(GoogleLoginRequestDTO dto) {
        GoogleIdToken.Payload payload = verifyGoogleToken(dto.getToken());

        String email = payload.getEmail();
        if (email == null || email.isBlank()) {
            throw new BadCredentialsException("Google account does not provide an email");
        }

        String firstName = truncate(payload.get("given_name") != null ? payload.get("given_name").toString() : null, 30);
        String lastName = truncate(payload.get("family_name") != null ? payload.get("family_name").toString() : null, 30);
        String pictureUrl = truncate(extractClaim(payload, "picture"), 500);

        User user = userRepository
                .findByEmailIgnoreCase(email)
                .map(existing -> updateExistingUser(existing, pictureUrl))
                .orElseGet(() -> createGoogleUser(email, firstName, lastName, pictureUrl, dto.getActiveRole()));

        if (!user.isActive()) {
            throw new DisabledException("User account is disabled");
        }

        String activeRole = resolveSessionActiveRole(dto.getActiveRole(), user);
        if (!activeRole.equals(user.getActiveRole())) {
            user.setActiveRole(activeRole);
            user = userRepository.save(user);
        }

        if (emailVerificationService.requiresVerification(user)) {
            emailVerificationService.sendVerificationCode(user);
            UserSummaryDTO summary = userMapper.toSummaryDTO(user, activeRole);
            return AuthResponseDTO.verificationPending(
                    summary, "Te enviamos un código de verificación a tu correo.");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user, activeRole);
        String jwt = jwtService.generateToken(userDetails, activeRole);
        UserSummaryDTO summary = userMapper.toSummaryDTO(user, activeRole);
        return new AuthResponseDTO(jwt, summary);
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                            new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new BadCredentialsException("Invalid Google ID token");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException ex) {
            throw new BadCredentialsException("Failed to verify Google ID token", ex);
        }
    }

    private User createGoogleUser(
            String email, String firstName, String lastName, String pictureUrl, String requestedRole) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(generateUniqueUsername(email));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setProfilePicture(pictureUrl);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setIdentityProvider("google");
        user.setEmailVerified(false);
        UserRoleNormalizer.applyDualRoleCapabilities(
                user, normalizeRequestedRole(requestedRole, "student"));
        user.setActive(true);
        user.setDateJoined(Instant.now());
        user.setSpecialties(JsonNodeFactory.instance.objectNode());
        user.setSocialLinks(JsonNodeFactory.instance.objectNode());
        return userRepository.save(user);
    }

    private User updateExistingUser(User user, String pictureUrl) {
        UserRoleNormalizer.ensureDualCapabilities(user);
        boolean dirty = false;
        if (!user.isStudent() || !user.isInfoproductor()) {
            dirty = true;
        }
        if (pictureUrl != null
                && !pictureUrl.isBlank()
                && (user.getProfilePicture() == null || !pictureUrl.equals(user.getProfilePicture()))) {
            user.setProfilePicture(pictureUrl);
            dirty = true;
        }
        return dirty ? userRepository.save(user) : user;
    }

    private String generateUniqueUsername(String email) {
        String localPart = email.substring(0, email.indexOf('@'));
        String base = localPart.replaceAll("[^a-zA-Z0-9_]", "");
        if (base.isBlank()) {
            base = "user";
        }
        if (base.length() > 140) {
            base = base.substring(0, 140);
        }

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private String extractClaim(GoogleIdToken.Payload payload, String name) {
        Object value = payload.get(name);
        return value != null ? value.toString() : null;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String resolveSessionActiveRole(String requestedRole, User user) {
        String normalized = normalizeRequestedRole(requestedRole, null);
        if (normalized != null) {
            UserRoleNormalizer.ensureDualCapabilities(user);
            return normalized;
        }
        return RoleAuthorityResolver.defaultActiveRole(user);
    }

    private String normalizeRequestedRole(String requestedRole, String fallback) {
        if (requestedRole == null || requestedRole.isBlank()) {
            return fallback;
        }
        String role = requestedRole.trim().toLowerCase(java.util.Locale.ROOT);
        if ("infoproductor".equals(role) || "student".equals(role)) {
            return role;
        }
        return fallback;
    }
}
