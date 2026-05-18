package com.skillnet.web.controller;

import com.skillnet.mapper.UserMapper;
import com.skillnet.persistence.entity.core.User;
import com.skillnet.persistence.repository.UserRepository;
import com.skillnet.security.CustomUserDetails;
import com.skillnet.security.JwtService;
import com.skillnet.service.GoogleAuthService;
import com.skillnet.service.UserService;
import com.skillnet.web.dto.request.GoogleLoginRequestDTO;
import com.skillnet.web.dto.request.LoginRequestDTO;
import com.skillnet.web.dto.request.UserRequestDTO;
import com.skillnet.web.dto.response.AuthResponseDTO;
import com.skillnet.web.dto.response.UserResponseDTO;
import com.skillnet.web.dto.response.UserSummaryDTO;
import com.skillnet.web.validation.OnCreate;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserService userService,
            UserMapper userMapper,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            GoogleAuthService googleAuthService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.googleAuthService = googleAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword());
        authenticationManager.authenticate(authToken);

        User user = userRepository
                .findByEmailIgnoreCase(dto.getEmail())
                .orElseThrow();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String jwt = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(buildAuthResponse(jwt, user));
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
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String jwt = jwtService.generateToken(userDetails);

        return ResponseEntity.status(HttpStatus.CREATED).body(buildAuthResponse(jwt, user));
    }

    private AuthResponseDTO buildAuthResponse(String jwt, User user) {
        UserSummaryDTO summary = userMapper.toSummaryDTO(user);
        return new AuthResponseDTO(jwt, summary);
    }
}
