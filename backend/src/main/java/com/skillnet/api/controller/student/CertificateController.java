package com.skillnet.api.controller.student;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.student.CertificateService;
import com.skillnet.web.dto.response.CertificateCheckResponseDTO;
import com.skillnet.web.dto.response.CertificateOverviewResponseDTO;
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
@RequestMapping("/api/v1/student/certificates")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @GetMapping
    public ResponseEntity<CertificateOverviewResponseDTO> getOverview(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(certificateService.getOverview(userDetails.getId()));
    }

    @GetMapping("/courses/{courseId}/check")
    public ResponseEntity<CertificateCheckResponseDTO> checkCourse(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long courseId) {
        requireAuth(userDetails);
        return ResponseEntity.ok(certificateService.checkCourseCertificate(userDetails.getId(), courseId));
    }

    private void requireAuth(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
    }
}
