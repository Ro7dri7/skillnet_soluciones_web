package com.skillnet.api.controller.student;

import com.skillnet.api.dto.student.MyCourseResponseDTO;
import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.student.StudentLearningService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/student")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StudentLearningController {

    private final StudentLearningService studentLearningService;

    @GetMapping("/my-learning")
    public ResponseEntity<List<MyCourseResponseDTO>> getMyCourses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return ResponseEntity.ok(studentLearningService.getMyCourses(userDetails.getId()));
    }
}
