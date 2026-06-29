package com.skillnet.service.auth;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.skillnet.domain.AuditAction;
import com.skillnet.persistence.entity.core.TwoFactorAuth;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.TwoFactorAuthRepository;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.security.CustomUserDetails;
import com.skillnet.security.JwtService;
import com.skillnet.security.RoleAuthorityResolver;
import com.skillnet.service.AuditService;
import com.skillnet.web.dto.request.TwoFactorDisableRequestDTO;
import com.skillnet.web.dto.request.TwoFactorVerifyLoginRequestDTO;
import com.skillnet.web.dto.response.AuthResponseDTO;
import com.skillnet.web.dto.response.TwoFactorEnableResponseDTO;
import com.skillnet.web.dto.response.TwoFactorStatusResponseDTO;
import com.skillnet.web.dto.response.UserSummaryDTO;
import com.skillnet.mapper.UserMapper;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.util.Utils;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TwoFactorAuthService {

    private static final String METHOD_APP = "app";

    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final TwoFactorLoginAttemptService loginAttemptService;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier =
            new DefaultCodeVerifier(new DefaultCodeGenerator(HashingAlgorithm.SHA1), new SystemTimeProvider());

    @Value("${skillnet.2fa.issuer:SkillNet}")
    private String issuerName;

    @Transactional(readOnly = true)
    public boolean isEnabledForUser(Long userId) {
        return twoFactorAuthRepository.existsByUser_IdAndEnabledTrue(userId);
    }

    @Transactional(readOnly = true)
    public TwoFactorStatusResponseDTO getStatus(Long userId) {
        User user = requireUser(userId);
        boolean passwordRequired = requiresPasswordForDisable(user);
        return twoFactorAuthRepository
                .findByUser_Id(userId)
                .map(tf -> TwoFactorStatusResponseDTO.builder()
                        .enabled(tf.isEnabled())
                        .method(tf.getMethod())
                        .passwordRequiredForDisable(passwordRequired)
                        .build())
                .orElse(TwoFactorStatusResponseDTO.builder()
                        .enabled(false)
                        .method(METHOD_APP)
                        .passwordRequiredForDisable(passwordRequired)
                        .build());
    }

    @Transactional
    public TwoFactorEnableResponseDTO beginSetup(Long userId) {
        User user = requireUser(userId);
        String secret = secretGenerator.generate();

        TwoFactorAuth record = twoFactorAuthRepository
                .findByUser_Id(userId)
                .orElseGet(() -> {
                    TwoFactorAuth created = new TwoFactorAuth();
                    created.setUser(user);
                    created.setCreatedAt(Instant.now());
                    created.setBackupCodes(JsonNodeFactory.instance.arrayNode());
                    return created;
                });

        record.setSecretKey(secret);
        record.setMethod(METHOD_APP);
        record.setEnabled(false);
        record.setUpdatedAt(Instant.now());
        twoFactorAuthRepository.save(record);

        String qrCode = buildQrDataUri(user.getEmail(), secret);
        return TwoFactorEnableResponseDTO.builder()
                .method(METHOD_APP)
                .secret(secret)
                .qrCode(qrCode)
                .build();
    }

    @Transactional
    public void confirmSetup(Long userId, String code) {
        TwoFactorAuth record = requireRecord(userId);
        if (!verifyCode(record.getSecretKey(), code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido");
        }
        record.setEnabled(true);
        record.setUpdatedAt(Instant.now());
        twoFactorAuthRepository.save(record);

        User user = record.getUser();
        auditService.logAction(
                AuditAction.ENABLE_2FA,
                AuditAction.ENTITY_USER,
                user.getId(),
                user.getEmail(),
                "2FA TOTP activado");
    }

    @Transactional
    public void disable(Long userId, TwoFactorDisableRequestDTO dto) {
        User user = requireUser(userId);
        if (requiresPasswordForDisable(user)) {
            String password = dto.getPassword();
            if (password == null || password.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria");
            }
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contraseña incorrecta");
            }
        }

        TwoFactorAuth record = twoFactorAuthRepository
                .findByUser_Id(userId)
                .filter(TwoFactorAuth::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "2FA no está activo"));

        if (!verifyCode(record.getSecretKey(), dto.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido");
        }

        twoFactorAuthRepository.delete(record);
        auditService.logAction(
                AuditAction.DISABLE_2FA,
                AuditAction.ENTITY_USER,
                user.getId(),
                user.getEmail(),
                "2FA TOTP desactivado");
    }

    private boolean requiresPasswordForDisable(User user) {
        String provider = user.getIdentityProvider();
        return provider == null
                || provider.isBlank()
                || !"google".equalsIgnoreCase(provider.trim());
    }

    public AuthResponseDTO pendingLoginResponse(User user) {
        String activeRole = RoleAuthorityResolver.defaultActiveRole(user);
        UserSummaryDTO summary = userMapper.toSummaryDTO(user, activeRole);
        return AuthResponseDTO.twoFactorPending(
                summary, jwtService.generatePending2FaToken(user.getId()), METHOD_APP);
    }

    @Transactional
    public AuthResponseDTO completeLogin(TwoFactorVerifyLoginRequestDTO dto) {
        Long userId;
        try {
            userId = jwtService.resolvePending2FaUserId(dto.getTwoFactorToken());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sesión 2FA inválida o expirada");
        }

        if (loginAttemptService.isLocked(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Vuelve a iniciar sesión.");
        }

        User user = userRepository
                .findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sesión 2FA inválida"));

        TwoFactorAuth record = twoFactorAuthRepository
                .findByUser_Id(userId)
                .filter(TwoFactorAuth::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "2FA no configurado"));

        if (!verifyCode(record.getSecretKey(), dto.getCode())) {
            loginAttemptService.recordFailure(userId);
            if (loginAttemptService.isLocked(userId)) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Vuelve a iniciar sesión.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido");
        }

        loginAttemptService.clearFailures(userId);
        String activeRole = RoleAuthorityResolver.defaultActiveRole(user);
        CustomUserDetails userDetails = new CustomUserDetails(user, activeRole);
        String jwt = jwtService.generateToken(userDetails, activeRole);
        UserSummaryDTO summary = userMapper.toSummaryDTO(user, activeRole);
        summary.setInfoproductor(user.isInfoproductor());
        summary.setStudent(user.isStudent());

        auditService.logAction(
                AuditAction.LOGIN_2FA,
                AuditAction.ENTITY_USER,
                user.getId(),
                user.getEmail(),
                "Sesión iniciada con 2FA TOTP");

        return new AuthResponseDTO(jwt, summary);
    }

    private boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        return codeVerifier.isValidCode(secret, code.trim());
    }

    private String buildQrDataUri(String email, String secret) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(issuerName)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        QrGenerator generator = new ZxingPngQrGenerator();
        try {
            byte[] image = generator.generate(data);
            return Utils.getDataUriForImage(image, generator.getImageMimeType());
        } catch (QrGenerationException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el código QR");
        }
    }

    private User requireUser(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private TwoFactorAuth requireRecord(Long userId) {
        return twoFactorAuthRepository
                .findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inicia la configuración 2FA primero"));
    }
}
