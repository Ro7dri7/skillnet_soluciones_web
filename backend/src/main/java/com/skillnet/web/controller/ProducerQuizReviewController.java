package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.producer.ProducerQuizReviewService;
import com.skillnet.web.dto.request.GradeQuizAnswerRequestDTO;
import com.skillnet.web.dto.request.QuizReviewRequestDTO;
import com.skillnet.web.dto.response.GradeQuizAnswerResponseDTO;
import com.skillnet.web.dto.response.QuizSubmissionReviewResponseDTO;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/producer/quiz-submissions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
public class ProducerQuizReviewController {

    private final ProducerQuizReviewService producerQuizReviewService;

    @GetMapping
    public ResponseEntity<List<QuizSubmissionReviewResponseDTO>> listPending(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(producerQuizReviewService.listPending(userDetails.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizSubmissionReviewResponseDTO> getSubmission(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long id) {
        requireAuth(userDetails);
        return ResponseEntity.ok(producerQuizReviewService.getSubmission(userDetails.getId(), id));
    }

    @PostMapping("/{id}/grade-answer")
    public ResponseEntity<GradeQuizAnswerResponseDTO> gradeAnswer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody GradeQuizAnswerRequestDTO dto) {
        requireAuth(userDetails);
        return ResponseEntity.ok(producerQuizReviewService.gradeAnswer(userDetails.getId(), id, dto));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<QuizSubmissionReviewResponseDTO> reviewSubmission(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody QuizReviewRequestDTO dto) {
        requireAuth(userDetails);
        return ResponseEntity.ok(producerQuizReviewService.reviewSubmission(userDetails.getId(), id, dto));
    }

    private void requireAuth(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
    }
}
