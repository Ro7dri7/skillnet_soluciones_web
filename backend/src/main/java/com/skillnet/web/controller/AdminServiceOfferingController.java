package com.skillnet.web.controller;

import com.skillnet.service.admin.AdminServiceOfferingService;
import com.skillnet.web.dto.request.ServiceOfferingRequestDTO;
import com.skillnet.web.dto.response.ServiceOfferingResponseDTO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/admin/service-offerings")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminServiceOfferingController {

    private final AdminServiceOfferingService adminServiceOfferingService;

    @GetMapping
    public ResponseEntity<List<ServiceOfferingResponseDTO>> listAll() {
        return ResponseEntity.ok(adminServiceOfferingService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceOfferingResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(adminServiceOfferingService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ServiceOfferingResponseDTO> create(@Valid @RequestBody ServiceOfferingRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminServiceOfferingService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceOfferingResponseDTO> update(
            @PathVariable Long id, @Valid @RequestBody ServiceOfferingRequestDTO dto) {
        return ResponseEntity.ok(adminServiceOfferingService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminServiceOfferingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
