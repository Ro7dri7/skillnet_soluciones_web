package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.producer.ReferralService;
import com.skillnet.web.dto.response.ReferralLinkResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/producer/courses")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
public class ReferralController {

    private final ReferralService referralService;

    @GetMapping("/{courseId}/referral-link")
    public ResponseEntity<ReferralLinkResponseDTO> getReferralLink(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long courseId) {
        requireAuth(userDetails);
        return ResponseEntity.ok(referralService.getReferralLink(userDetails.getId(), courseId));
    }

    private void requireAuth(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
    }
}
