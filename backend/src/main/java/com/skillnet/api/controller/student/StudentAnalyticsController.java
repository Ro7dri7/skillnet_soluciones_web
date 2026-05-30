package com.skillnet.api.controller.student;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.student.StudentAnalyticsService;
import com.skillnet.web.dto.response.analytics.StudentAnalyticsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/student/analytics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StudentAnalyticsController {

    private final StudentAnalyticsService studentAnalyticsService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<StudentAnalyticsDTO> getAnalytics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return ResponseEntity.ok(studentAnalyticsService.getAnalytics(userDetails.getId(), year, month));
    }
}
