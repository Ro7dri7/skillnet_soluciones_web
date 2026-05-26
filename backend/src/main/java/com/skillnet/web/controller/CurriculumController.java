package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.CurriculumService;
import com.skillnet.web.dto.request.CreateCurriculumLessonRequestDTO;
import com.skillnet.web.dto.request.CreateSectionRequestDTO;
import com.skillnet.web.dto.request.LessonUpdateRequestDTO;
import com.skillnet.web.dto.request.SectionUpdateRequestDTO;
import com.skillnet.web.dto.response.CurriculumLessonResponseDTO;
import com.skillnet.web.dto.response.CurriculumModuleResponseDTO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class CurriculumController {

    private final CurriculumService curriculumService;

    public CurriculumController(CurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    @GetMapping("/courses/{courseId}/curriculum")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<List<CurriculumModuleResponseDTO>> getCurriculum(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long courseId) {
        return ResponseEntity.ok(curriculumService.getCurriculum(courseId, userDetails.getId()));
    }

    @PostMapping("/courses/{courseId}/sections")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<CurriculumModuleResponseDTO> createSection(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long courseId,
            @Valid @RequestBody CreateSectionRequestDTO request) {
        CurriculumModuleResponseDTO created =
                curriculumService.createSection(courseId, userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/sections/{sectionId}/lessons")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<CurriculumLessonResponseDTO> createLesson(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sectionId,
            @Valid @RequestBody CreateCurriculumLessonRequestDTO request) {
        CurriculumLessonResponseDTO created =
                curriculumService.createLesson(sectionId, userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Actualización parcial (autoguardado): título, contenido, quiz, orden. */
    @PutMapping("/lessons/{lessonId}")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<CurriculumLessonResponseDTO> patchLesson(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long lessonId,
            @Valid @RequestBody LessonUpdateRequestDTO request) {
        CurriculumLessonResponseDTO updated =
                curriculumService.patchLesson(lessonId, userDetails.getId(), request);
        return ResponseEntity.ok(updated);
    }

    /** Compatibilidad con clientes anteriores. */
    @PutMapping("/curriculum/lessons/{lessonId}")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<CurriculumLessonResponseDTO> updateLessonLegacy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long lessonId,
            @Valid @RequestBody CreateCurriculumLessonRequestDTO request) {
        CurriculumLessonResponseDTO updated =
                curriculumService.updateLesson(lessonId, userDetails.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/sections/{sectionId}")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<CurriculumModuleResponseDTO> updateSection(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sectionId,
            @Valid @RequestBody SectionUpdateRequestDTO request) {
        CurriculumModuleResponseDTO updated =
                curriculumService.updateSection(sectionId, userDetails.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/sections/{sectionId}")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<Void> deleteSection(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long sectionId) {
        curriculumService.deleteSection(sectionId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/lessons/{lessonId}")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<Void> deleteLesson(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long lessonId) {
        curriculumService.deleteLesson(lessonId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
