package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.producer.PodcastGenerationService;
import com.skillnet.web.dto.request.PodcastAttachRequestDTO;
import com.skillnet.web.dto.request.PodcastGenerateRequestDTO;
import com.skillnet.web.dto.request.PodcastSynthesizeRequestDTO;
import com.skillnet.web.dto.response.PodcastJobResponseDTO;
import jakarta.validation.Valid;
import java.util.Map;
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
@RequestMapping("/api/v1/producer/podcast")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
public class ProducerPodcastController {

    private final PodcastGenerationService podcastGenerationService;

    @PostMapping("/generate")
    public ResponseEntity<PodcastJobResponseDTO> generate(
            @Valid @RequestBody PodcastGenerateRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(podcastGenerationService.startGeneration(userDetails.getId(), request));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<PodcastJobResponseDTO> jobStatus(
            @PathVariable Long jobId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(podcastGenerationService.getJob(userDetails.getId(), jobId));
    }

    @PostMapping("/jobs/{jobId}/attach")
    public ResponseEntity<Map<String, Object>> attach(
            @PathVariable Long jobId,
            @RequestBody PodcastAttachRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(podcastGenerationService.attachToLesson(userDetails.getId(), jobId, request));
    }

    @PostMapping("/jobs/{jobId}/synthesize")
    public ResponseEntity<PodcastJobResponseDTO> synthesize(
            @PathVariable Long jobId,
            @Valid @RequestBody PodcastSynthesizeRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(
                podcastGenerationService.synthesizeFromScript(
                        userDetails.getId(), jobId, request.getApprovedTranscript()));
    }

    private void requireAuth(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
    }
}
