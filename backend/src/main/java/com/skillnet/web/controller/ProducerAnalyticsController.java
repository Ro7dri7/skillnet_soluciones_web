package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.ProducerAnalyticsService;
import com.skillnet.web.dto.response.analytics.ProducerAnalyticsDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/producer/analytics")
@CrossOrigin(origins = "*")
public class ProducerAnalyticsController {

    private final ProducerAnalyticsService producerAnalyticsService;

    public ProducerAnalyticsController(ProducerAnalyticsService producerAnalyticsService) {
        this.producerAnalyticsService = producerAnalyticsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ProducerAnalyticsDTO> getAnalytics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long professorId = userDetails != null ? userDetails.getId() : null;
        if (professorId == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        ProducerAnalyticsDTO analytics = producerAnalyticsService.getAnalytics(professorId, year, month);
        return ResponseEntity.ok(analytics);
    }
}
