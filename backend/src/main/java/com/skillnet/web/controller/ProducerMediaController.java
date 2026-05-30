package com.skillnet.web.controller;

import com.skillnet.security.CustomUserDetails;
import com.skillnet.service.ProducerCourseService;
import com.skillnet.web.dto.response.MediaUploadResponseDTO;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/producer/media")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_INFOPRODUCTOR', 'ROLE_ADMIN')")
public class ProducerMediaController {

    private final ProducerCourseService producerCourseService;

    public ProducerMediaController(ProducerCourseService producerCourseService) {
        this.producerCourseService = producerCourseService;
    }

    @PostMapping("/upload")
    public ResponseEntity<MediaUploadResponseDTO> upload(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long courseId,
            @RequestParam String type,
            @RequestParam("file") MultipartFile file)
            throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String kind = mapUploadType(type);
        var result = producerCourseService.uploadCourseMedia(
                courseId,
                userDetails.getId(),
                kind,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getInputStream(),
                file.getSize());

        MediaUploadResponseDTO dto = new MediaUploadResponseDTO();
        dto.setUrl(result.getPublicUrl());
        dto.setStorageKey(result.getStorageKey());
        dto.setType(type);
        return ResponseEntity.ok(dto);
    }

    private static String mapUploadType(String type) {
        if (type == null) {
            return "cover";
        }
        return switch (type.trim().toLowerCase()) {
            case "thumbnail", "cover", "image" -> "cover";
            case "video", "promo_video" -> "promo_video";
            case "pdf", "resource", "audio" -> "resource";
            default -> type.trim().toLowerCase();
        };
    }
}
