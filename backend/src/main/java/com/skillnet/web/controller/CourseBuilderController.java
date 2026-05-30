package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.CourseBuilderService;
import com.skillnet.web.dto.request.CourseBuilderRequestDTO;
import com.skillnet.web.dto.response.CourseBuilderResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses/builder")
@CrossOrigin(origins = "*")
public class CourseBuilderController {

    private final CourseBuilderService courseBuilderService;

    public CourseBuilderController(CourseBuilderService courseBuilderService) {
        this.courseBuilderService = courseBuilderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<CourseBuilderResponseDTO> saveDraft(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CourseBuilderRequestDTO request) {
        CourseBuilderResponseDTO saved = courseBuilderService.saveDraft(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{courseId}")
    @PreAuthorize("hasAnyAuthority('ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<CourseBuilderResponseDTO> getByCourseId(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long courseId) {
        return ResponseEntity.ok(courseBuilderService.getByCourseId(courseId, userDetails.getId()));
    }
}
