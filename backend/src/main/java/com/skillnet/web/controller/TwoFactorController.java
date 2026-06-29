package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.auth.TwoFactorAuthService;
import com.skillnet.web.dto.request.TwoFactorCodeRequestDTO;
import com.skillnet.web.dto.request.TwoFactorDisableRequestDTO;
import com.skillnet.web.dto.request.TwoFactorVerifyLoginRequestDTO;
import com.skillnet.web.dto.response.AuthResponseDTO;
import com.skillnet.web.dto.response.TwoFactorEnableResponseDTO;
import com.skillnet.web.dto.response.TwoFactorStatusResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth/2fa")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorAuthService twoFactorAuthService;

    @GetMapping("/status")
    public ResponseEntity<TwoFactorStatusResponseDTO> status(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(twoFactorAuthService.getStatus(requireUserId(userDetails)));
    }

    @PostMapping("/enable")
    public ResponseEntity<TwoFactorEnableResponseDTO> enable(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(twoFactorAuthService.beginSetup(requireUserId(userDetails)));
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verify(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TwoFactorCodeRequestDTO dto) {
        twoFactorAuthService.confirmSetup(requireUserId(userDetails), dto.getCode());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/disable")
    public ResponseEntity<Void> disable(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TwoFactorDisableRequestDTO dto) {
        twoFactorAuthService.disable(requireUserId(userDetails), dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-login")
    public ResponseEntity<AuthResponseDTO> verifyLogin(@Valid @RequestBody TwoFactorVerifyLoginRequestDTO dto) {
        return ResponseEntity.ok(twoFactorAuthService.completeLogin(dto));
    }

    private Long requireUserId(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return userDetails.getId();
    }
}
