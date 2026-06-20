package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.CourseService;
import com.skillnet.service.ProducerCourseService;
import com.skillnet.web.dto.request.CourseRequestDTO;
import com.skillnet.web.dto.request.UpdateCourseBasicsRequestDTO;
import com.skillnet.web.dto.response.CourseBasicsResponseDTO;
import com.skillnet.web.dto.response.CourseResponseDTO;
import com.skillnet.web.dto.response.ProducerCourseSummaryDTO;
import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses")
@CrossOrigin(origins = "*")
public class CourseController {

    private final CourseService courseService;
    private final ProducerCourseService producerCourseService;

    public CourseController(CourseService courseService, ProducerCourseService producerCourseService) {
        this.courseService = courseService;
        this.producerCourseService = producerCourseService;
    }

    @GetMapping
    public ResponseEntity<List<CourseResponseDTO>> findAll() {
        return ResponseEntity.ok(courseService.findAll());
    }

    @GetMapping(value = "/by-slug", params = "slug")
    public ResponseEntity<CourseResponseDTO> findBySlugQuery(@RequestParam("slug") String slug) {
        return ResponseEntity.ok(courseService.findBySlugVariants(slug)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with slug: " + slug)));
    }

    @GetMapping("/by-slug/{format}/{slugStem}")
    public ResponseEntity<CourseResponseDTO> findBySlugWithFormat(
            @PathVariable String format, @PathVariable String slugStem) {
        String joined = com.skillnet.util.CourseSlugUtils.joinRouteSlug(format, slugStem);
        return ResponseEntity.ok(courseService.findBySlugVariants(joined)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with slug: " + joined)));
    }

    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<CourseResponseDTO> findBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(courseService.findBySlugVariants(slug)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with slug: " + slug)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_INFOPRODUCTOR')")
    public ResponseEntity<CourseResponseDTO> create(@Valid @RequestBody CourseRequestDTO dto) {
        CourseResponseDTO created = courseService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_INFOPRODUCTOR')")
    public ResponseEntity<CourseResponseDTO> update(
            @PathVariable Long id, @Valid @RequestBody CourseRequestDTO dto) {
        return ResponseEntity.ok(courseService.update(id, dto)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_INFOPRODUCTOR')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        courseService.deleteCourse(id, userDetails.getId(), userDetails.getRole());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{courseId}/publish")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ProducerCourseSummaryDTO> publishCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProducerCourseSummaryDTO updated =
                producerCourseService.publishCourse(courseId, userDetails.getId());
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{courseId}/draft")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<ProducerCourseSummaryDTO> unpublishCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProducerCourseSummaryDTO updated =
                producerCourseService.unpublishCourse(courseId, userDetails.getId());
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{courseId}/basics")
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
    public ResponseEntity<CourseBasicsResponseDTO> updateBasics(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateCourseBasicsRequestDTO dto) {
        CourseBasicsResponseDTO updated =
                producerCourseService.updateBasics(courseId, userDetails.getId(), dto);
        return ResponseEntity.ok(updated);
    }
}
