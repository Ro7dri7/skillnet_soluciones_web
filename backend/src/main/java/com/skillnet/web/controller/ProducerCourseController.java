package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.ProducerCourseService;
import com.skillnet.web.dto.request.CreateCourseRequestDTO;
import com.skillnet.web.dto.response.ProducerCourseSummaryDTO;
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
import org.springframework.web.multipart.MultipartFile;
import com.skillnet.web.dto.request.CreateCourseCouponRequestDTO;
import com.skillnet.web.dto.request.UpdateCourseMessagesRequestDTO;
import com.skillnet.web.dto.request.UpdateCoursePricingRequestDTO;
import com.skillnet.web.dto.response.CourseCouponResponseDTO;
import com.skillnet.web.dto.response.CourseMediaUploadResponseDTO;
import com.skillnet.web.dto.response.CourseMessagesResponseDTO;
import com.skillnet.web.dto.response.CoursePricingResponseDTO;
import com.skillnet.web.dto.request.UpdateCourseBasicsRequestDTO;
import com.skillnet.web.dto.response.CourseBasicsResponseDTO;

@RestController
@RequestMapping("/api/v1/producer/courses")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
public class ProducerCourseController {

    private final ProducerCourseService producerCourseService;

    public ProducerCourseController(ProducerCourseService producerCourseService) {
        this.producerCourseService = producerCourseService;
    }

    @PostMapping
    public ResponseEntity<ProducerCourseSummaryDTO> createDraftCourse(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateCourseRequestDTO request) {
        ProducerCourseSummaryDTO created =
                producerCourseService.createDraftCourse(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ProducerCourseSummaryDTO>> getMyCourses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ProducerCourseSummaryDTO> courses =
                producerCourseService.getMyCourses(userDetails.getId());
        return ResponseEntity.ok(courses);
    }

    @PutMapping("/{courseId}/basics")
    public ResponseEntity<CourseBasicsResponseDTO> updateBasics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseBasicsRequestDTO dto) {
        CourseBasicsResponseDTO updated =
                producerCourseService.updateBasics(courseId, userDetails.getId(), dto);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{courseId}/media")
    public ResponseEntity<CourseMediaUploadResponseDTO> uploadMedia(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long courseId,
            @RequestParam("kind") String kind,
            @RequestParam("file") MultipartFile file)
            throws java.io.IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        CourseMediaUploadResponseDTO result = producerCourseService.uploadCourseMedia(
                courseId,
                userDetails.getId(),
                kind,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getInputStream(),
                file.getSize());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{courseId}/pricing")
    public ResponseEntity<CoursePricingResponseDTO> updatePricing(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCoursePricingRequestDTO dto) {
        return ResponseEntity.ok(
                producerCourseService.updatePricing(courseId, userDetails.getId(), dto));
    }

    @PutMapping("/{courseId}/messages")
    public ResponseEntity<CourseMessagesResponseDTO> updateMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseMessagesRequestDTO dto) {
        return ResponseEntity.ok(
                producerCourseService.updateMessages(courseId, userDetails.getId(), dto));
    }

    @GetMapping("/{courseId}/coupons")
    public ResponseEntity<List<CourseCouponResponseDTO>> listCoupons(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long courseId) {
        return ResponseEntity.ok(producerCourseService.listCoupons(courseId, userDetails.getId()));
    }

    @PostMapping("/{courseId}/coupons")
    public ResponseEntity<CourseCouponResponseDTO> createCoupon(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long courseId,
            @Valid @RequestBody CreateCourseCouponRequestDTO dto) {
        CourseCouponResponseDTO created =
                producerCourseService.createCoupon(courseId, userDetails.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{courseId}/coupons/{couponId}")
    public ResponseEntity<Void> deleteCoupon(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long courseId,
            @PathVariable Long couponId) {
        producerCourseService.deleteCoupon(courseId, userDetails.getId(), couponId);
        return ResponseEntity.noContent().build();
    }
}
