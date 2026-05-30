package com.skillnet.web.controller;

import com.skillnet.service.EnrollmentService;
import com.skillnet.web.dto.request.EnrollmentRequestDTO;
import com.skillnet.web.dto.response.EnrollmentResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollments")
@CrossOrigin(origins = "*")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public ResponseEntity<List<EnrollmentResponseDTO>> findAll() {
        return ResponseEntity.ok(enrollmentService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Enrollment not found with id: " + id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_INFOPRODUCTOR')")
    public ResponseEntity<EnrollmentResponseDTO> create(@Valid @RequestBody EnrollmentRequestDTO dto) {
        EnrollmentResponseDTO created = enrollmentService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_INFOPRODUCTOR')")
    public ResponseEntity<EnrollmentResponseDTO> update(
            @PathVariable Long id, @Valid @RequestBody EnrollmentRequestDTO dto) {
        return ResponseEntity.ok(enrollmentService.update(id, dto)
                .orElseThrow(() -> new EntityNotFoundException("Enrollment not found with id: " + id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_INFOPRODUCTOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (enrollmentService.findById(id).isEmpty()) {
            throw new EntityNotFoundException("Enrollment not found with id: " + id);
        }
        enrollmentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
