package com.skillnet.web.controller;

import com.skillnet.service.UserService;
import com.skillnet.web.dto.request.UserRequestDTO;
import com.skillnet.web.dto.response.UserResponseDTO;
import com.skillnet.web.validation.OnCreate;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<List<UserResponseDTO>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id)));
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> create(
            @Validated(OnCreate.class) @Valid @RequestBody UserRequestDTO dto) {
        UserResponseDTO created = userService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin') or authentication.principal.id == #id")
    public ResponseEntity<UserResponseDTO> update(
            @PathVariable Long id, @Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.ok(userService.update(id, dto)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin') or authentication.principal.id == #id")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (userService.findById(id).isEmpty()) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
