package com.skillnet.web.controller;

import com.skillnet.service.LessonService;
import com.skillnet.web.dto.request.LessonRequestDTO;
import com.skillnet.web.dto.response.LessonResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lessons")
@CrossOrigin(origins = "*")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping
    public ResponseEntity<List<LessonResponseDTO>> findAll() {
        return ResponseEntity.ok(lessonService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LessonResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(lessonService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lesson not found with id: " + id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_INFOPRODUCTOR')")
    public ResponseEntity<LessonResponseDTO> create(@Valid @RequestBody LessonRequestDTO dto) {
        LessonResponseDTO created = lessonService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** PUT/DELETE de lecciones del temario: {@link CurriculumController} en {@code /api/v1/lessons/{id}}. */
}
