package com.skillnet.service.auth;

import com.skillnet.domain.AuditAction;
import com.skillnet.persistence.entity.core.PasswordResetToken;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.PasswordResetTokenRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.service.AuditService;
import com.skillnet.service.mail.EmailService;
import com.skillnet.web.dto.request.PasswordResetConfirmDTO;
import com.skillnet.web.dto.request.PasswordResetRequestDTO;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${skillnet.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Transactional
    public void requestReset(PasswordResetRequestDTO dto) {
        User user = userRepository
                .findByEmailIgnoreCase(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email no registrado"));

        String token = UUID.randomUUID().toString();
        Instant now = Instant.now();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setCreatedAt(now);
        resetToken.setExpiresAt(now.plus(24, ChronoUnit.HOURS));
        resetToken.setUsed(false);
        passwordResetTokenRepository.save(resetToken);

        String resetUrl = frontendBaseUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
        log.info("[password-reset] link sent for {}", user.getEmail());
    }

    @Transactional
    public void confirmReset(PasswordResetConfirmDTO dto) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndUsedFalse(dto.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o expirado"));

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expirado");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        auditService.logAction(
                AuditAction.PASSWORD_RESET,
                AuditAction.ENTITY_USER,
                user.getId(),
                user.getEmail(),
                "Contraseña restablecida vía enlace de recuperación");
    }
}
