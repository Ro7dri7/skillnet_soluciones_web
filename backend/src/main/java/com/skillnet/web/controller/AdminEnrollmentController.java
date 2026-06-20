package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.admin.AdminEnrollmentService;
import com.skillnet.web.dto.request.AdminEnrollmentCreateRequestDTO;
import com.skillnet.web.dto.request.UpdateUserRoleRequestDTO;
import com.skillnet.web.dto.response.EnrollmentResponseDTO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminEnrollmentController {

    private final AdminEnrollmentService adminEnrollmentService;

    @GetMapping("/enrollments")
    public ResponseEntity<List<EnrollmentResponseDTO>> listEnrollments() {
        return ResponseEntity.ok(adminEnrollmentService.listEnrollments());
    }

    @PostMapping("/enrollments")
    public ResponseEntity<EnrollmentResponseDTO> createEnrollment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AdminEnrollmentCreateRequestDTO dto) {
        requireAuth(userDetails);
        EnrollmentResponseDTO created =
                adminEnrollmentService.createEnrollment(dto, userDetails.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Void> updateUserRole(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequestDTO dto) {
        requireAuth(userDetails);
        adminEnrollmentService.updateUserRole(id, dto, userDetails.getEmail());
        return ResponseEntity.noContent().build();
    }

    private void requireAuth(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
    }
}
