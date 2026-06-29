package com.skillnet.service.auth;

import com.skillnet.persistence.entity.core.EmailVerificationToken;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.EmailVerificationTokenRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.service.mail.EmailService;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int CODE_TTL_MINUTES = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    @Transactional
    public void sendVerificationCode(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email no disponible para verificación");
        }
        tokenRepository.markAllUsedForUser(user.getId());

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setRegistrationCode(generateSixDigitCode());
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plus(CODE_TTL_MINUTES, ChronoUnit.MINUTES));
        token.setUsed(false);
        tokenRepository.save(token);

        emailService.sendVerificationCodeEmail(user.getEmail(), token.getRegistrationCode(), user.getFirstName());
    }

    @Transactional
    public User verifyCode(String email, String code) {
        User user = userRepository
                .findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código o email incorrectos"));

        EmailVerificationToken token = tokenRepository
                .findFirstByUser_IdAndRegistrationCodeAndUsedFalseAndExpiresAtAfter(
                        user.getId(), code.trim(), Instant.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido o expirado"));

        token.setUsed(true);
        tokenRepository.save(token);
        tokenRepository.markAllUsedForUser(user.getId());

        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    @Transactional
    public void resendForEmail(String email) {
        User user = userRepository
                .findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos una cuenta con ese email"));
        if (user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este email ya está verificado");
        }
        sendVerificationCode(user);
    }

    public boolean requiresVerification(User user) {
        if (user == null) {
            return false;
        }
        if (user.isSuperUser() || user.isStaff()) {
            return false;
        }
        return !user.isEmailVerified();
    }

    private static String generateSixDigitCode() {
        int value = RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}
