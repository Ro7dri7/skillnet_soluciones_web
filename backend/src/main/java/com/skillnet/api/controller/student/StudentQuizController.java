package com.skillnet.api.controller.student;

import com.skillnet.api.dto.student.QuizAttemptsResponseDTO;
import com.skillnet.api.dto.student.QuizSubmissionRequestDTO;
import com.skillnet.api.dto.student.QuizSubmissionResponseDTO;
import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.student.StudentQuizService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/student/lessons")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StudentQuizController {

    private final StudentQuizService studentQuizService;

    @PostMapping("/{lessonId}/quiz-submissions")
    public ResponseEntity<QuizSubmissionResponseDTO> submitQuiz(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long lessonId,
            @Valid @RequestBody QuizSubmissionRequestDTO dto) {
        requireAuth(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studentQuizService.submitQuiz(lessonId, userDetails.getId(), dto));
    }

    @GetMapping("/{lessonId}/quiz-submissions")
    public ResponseEntity<QuizAttemptsResponseDTO> getAttempts(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long lessonId) {
        requireAuth(userDetails);
        return ResponseEntity.ok(studentQuizService.getAttempts(lessonId, userDetails.getId()));
    }

    private void requireAuth(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
    }
}
