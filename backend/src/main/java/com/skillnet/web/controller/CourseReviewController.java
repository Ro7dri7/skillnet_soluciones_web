package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.review.CourseReviewService;
import com.skillnet.util.CourseSlugUtils;
import com.skillnet.web.dto.request.CreateCourseReviewRequestDTO;
import com.skillnet.web.dto.response.CourseReviewResponseDTO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/courses")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CourseReviewController {

    private final CourseReviewService courseReviewService;

    @GetMapping("/by-slug/{format}/{slugStem}/reviews")
    public ResponseEntity<List<CourseReviewResponseDTO>> listReviewsWithFormat(
            @PathVariable String format, @PathVariable String slugStem) {
        return listReviewsInternal(CourseSlugUtils.joinRouteSlug(format, slugStem));
    }

    @GetMapping(value = "/by-slug/reviews", params = "slug")
    public ResponseEntity<List<CourseReviewResponseDTO>> listReviewsByQuery(@RequestParam("slug") String slug) {
        return listReviewsInternal(slug);
    }

    @GetMapping("/by-slug/{slug}/reviews")
    public ResponseEntity<List<CourseReviewResponseDTO>> listReviews(@PathVariable String slug) {
        return listReviewsInternal(slug);
    }

    @PostMapping("/by-slug/{format}/{slugStem}/reviews")
    public ResponseEntity<CourseReviewResponseDTO> createReviewWithFormat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String format,
            @PathVariable String slugStem,
            @Valid @RequestBody CreateCourseReviewRequestDTO dto) {
        return createReviewInternal(userDetails, CourseSlugUtils.joinRouteSlug(format, slugStem), dto);
    }

    @PostMapping(value = "/by-slug/reviews", params = "slug")
    public ResponseEntity<CourseReviewResponseDTO> createReviewByQuery(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("slug") String slug,
            @Valid @RequestBody CreateCourseReviewRequestDTO dto) {
        return createReviewInternal(userDetails, slug, dto);
    }

    @PostMapping("/by-slug/{slug}/reviews")
    public ResponseEntity<CourseReviewResponseDTO> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String slug,
            @Valid @RequestBody CreateCourseReviewRequestDTO dto) {
        return createReviewInternal(userDetails, slug, dto);
    }

    private ResponseEntity<List<CourseReviewResponseDTO>> listReviewsInternal(String slug) {
        return ResponseEntity.ok(courseReviewService.listByCourseSlug(slug));
    }

    private ResponseEntity<CourseReviewResponseDTO> createReviewInternal(
            CustomUserDetails userDetails, String slug, CreateCourseReviewRequestDTO dto) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseReviewService.createReview(slug, userDetails.getId(), dto));
    }
}
