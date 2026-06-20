package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.producer.ProducerStudentProgressService;
import com.skillnet.web.dto.response.ProducerStudentProgressOverviewDTO;
import com.skillnet.web.dto.response.StudentProgressItemDTO;
import java.util.List;
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
@RequestMapping("/api/v1/producer/student-progress")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
public class ProducerStudentController {

    private final ProducerStudentProgressService producerStudentProgressService;

    @GetMapping("/overview")
    public ResponseEntity<ProducerStudentProgressOverviewDTO> getOverview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String course) {
        requireAuth(userDetails);
        return ResponseEntity.ok(producerStudentProgressService.getOverview(userDetails.getId(), course));
    }

    @GetMapping
    public ResponseEntity<List<StudentProgressItemDTO>> getStudentProgress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long courseId) {
        requireAuth(userDetails);
        return ResponseEntity.ok(producerStudentProgressService.getStudentProgress(userDetails.getId(), courseId));
    }

    private void requireAuth(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
    }
}
