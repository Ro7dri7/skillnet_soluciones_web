package com.skillnet.web.controller;

import com.skillnet.mapper.UserMapper;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.security.CustomUserDetails;
import com.skillnet.security.JwtService;
import com.skillnet.security.RoleAuthorityResolver;
import com.skillnet.service.AuthRoleService;
import com.skillnet.service.AuditService;
import com.skillnet.service.GoogleAuthService;
import com.skillnet.domain.AuditAction;
import com.skillnet.service.UserRoleNormalizer;
import com.skillnet.service.UserService;
import com.skillnet.service.auth.PasswordResetService;
import com.skillnet.web.dto.request.GoogleLoginRequestDTO;
import com.skillnet.web.dto.request.LoginRequestDTO;
import com.skillnet.web.dto.request.PasswordResetConfirmDTO;
import com.skillnet.web.dto.request.PasswordResetRequestDTO;
import com.skillnet.web.dto.request.SwitchRoleRequestDTO;
import com.skillnet.web.dto.request.UserRequestDTO;
import com.skillnet.web.dto.response.AuthResponseDTO;
import com.skillnet.web.dto.response.UserResponseDTO;
import com.skillnet.web.dto.response.UserSummaryDTO;
import com.skillnet.web.validation.OnCreate;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleAuthService googleAuthService;
    private final AuthRoleService authRoleService;
    private final PasswordResetService passwordResetService;
    private final AuditService auditService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserService userService,
            UserMapper userMapper,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            GoogleAuthService googleAuthService,
            AuthRoleService authRoleService,
            PasswordResetService passwordResetService,
            AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.googleAuthService = googleAuthService;
        this.authRoleService = authRoleService;
        this.passwordResetService = passwordResetService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword());
        authenticationManager.authenticate(authToken);

        User user = userRepository
                .findByEmailIgnoreCase(dto.getEmail())
                .orElseThrow();
        UserRoleNormalizer.ensureDualCapabilities(user);
        userRepository.save(user);
        return ResponseEntity.ok(buildAuthResponse(user, RoleAuthorityResolver.defaultActiveRole(user)));
    }

    @PostMapping("/switch-role")
    public ResponseEntity<AuthResponseDTO> switchRole(
            Authentication authentication, @Valid @RequestBody SwitchRoleRequestDTO dto) {
        Long userId = resolveAuthenticatedUserId(authentication);
        User user = userRepository.findById(userId).orElseThrow();
        String requestedRole = dto.getRole().trim().toLowerCase();

        if (!authRoleService.canAssumeRole(user, requestedRole)) {
            throw new AccessDeniedException(
                    "El rol «" + requestedRole + "» no está habilitado para esta cuenta");
        }

        try {
            UserRoleNormalizer.applyActiveRoleSwitch(user, requestedRole);
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException(ex.getMessage());
        }
        userRepository.save(user);

        auditService.logAction(
                AuditAction.SWITCH_ROLE,
                AuditAction.ENTITY_USER,
                user.getId(),
                user.getEmail(),
                "Rol activo cambiado a: " + requestedRole);

        return ResponseEntity.ok(buildAuthResponse(user, user.getActiveRole()));
    }

    private Long resolveAuthenticatedUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return principal.getId();
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponseDTO> loginWithGoogle(@Valid @RequestBody GoogleLoginRequestDTO dto) {
        return ResponseEntity.ok(googleAuthService.authenticate(dto));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Validated(OnCreate.class) @Valid @RequestBody UserRequestDTO dto) {
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        if (dto.getDateJoined() == null) {
            dto.setDateJoined(Instant.now());
        }

        UserResponseDTO created = userService.create(dto);
        User user = userRepository
                .findById(created.getId())
                .orElseThrow();
        UserRoleNormalizer.applyDualRoleCapabilities(user, user.getRole());
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildAuthResponse(user, RoleAuthorityResolver.defaultActiveRole(user)));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDTO dto) {
        passwordResetService.requestReset(dto);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmDTO dto) {
        passwordResetService.confirmReset(dto);
        return ResponseEntity.noContent().build();
    }

    private AuthResponseDTO buildAuthResponse(User user, String activeRole) {
        CustomUserDetails userDetails = new CustomUserDetails(user, activeRole);
        String jwt = jwtService.generateToken(userDetails, activeRole);
        UserSummaryDTO summary = userMapper.toSummaryDTO(user, activeRole);
        summary.setInfoproductor(user.isInfoproductor());
        summary.setStudent(user.isStudent());
        return new AuthResponseDTO(jwt, summary);
    }
}
