package com.skillnet.api.controller.student;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.student.StudentPaymentService;
import com.skillnet.web.dto.response.PaymentResponseDTO;
import com.skillnet.web.dto.response.PaymentStatusResponseDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StudentPaymentController {

    private final StudentPaymentService studentPaymentService;

    @GetMapping("/api/v1/student/payments")
    public ResponseEntity<List<PaymentResponseDTO>> getMyPayments(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(studentPaymentService.getMyPayments(userDetails.getId()));
    }

    @GetMapping("/api/v1/student/payments/{id}/status")
    public ResponseEntity<PaymentStatusResponseDTO> getPaymentStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long id) {
        requireAuth(userDetails);
        return ResponseEntity.ok(studentPaymentService.getPaymentStatus(userDetails.getId(), id));
    }

    private void requireAuth(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
    }
}
