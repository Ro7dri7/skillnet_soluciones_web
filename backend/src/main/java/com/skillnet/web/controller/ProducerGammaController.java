package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.producer.GammaReferencePdfService;
import com.skillnet.service.producer.GammaService;
import com.skillnet.web.dto.request.GammaGenerateRequestDTO;
import com.skillnet.web.dto.response.GammaGenerationResponseDTO;
import com.skillnet.web.dto.response.GammaReferenceExtractResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/producer/gamma")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
public class ProducerGammaController {

    private final GammaService gammaService;
    private final GammaReferencePdfService gammaReferencePdfService;

    @PostMapping("/generate")
    public ResponseEntity<GammaGenerationResponseDTO> generate(
            @Valid @RequestBody GammaGenerateRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(gammaService.startGeneration(userDetails.getId(), request));
    }

    @PostMapping(value = "/extract-reference", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GammaReferenceExtractResponseDTO> extractReference(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        String text = gammaReferencePdfService.extractText(file);
        return ResponseEntity.ok(GammaReferenceExtractResponseDTO.builder()
                .extractedText(text)
                .characterCount(text.length())
                .build());
    }

    @GetMapping("/status/{generationId}")
    public ResponseEntity<GammaGenerationResponseDTO> status(
            @PathVariable String generationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAuth(userDetails);
        return ResponseEntity.ok(gammaService.getStatus(userDetails.getId(), generationId));
    }

    private void requireAuth(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
    }
}
