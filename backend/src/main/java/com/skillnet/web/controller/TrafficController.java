package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.producer.TrafficAnalyticsService;
import com.skillnet.web.dto.response.TrafficAnalyticsResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/producer/traffic-analytics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
public class TrafficController {

    private final TrafficAnalyticsService trafficAnalyticsService;

    @GetMapping
    public ResponseEntity<TrafficAnalyticsResponseDTO> getTrafficAnalytics(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(trafficAnalyticsService.getTrafficAnalytics(userDetails.getId()));
    }

    private void requireAuth(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
    }
}
