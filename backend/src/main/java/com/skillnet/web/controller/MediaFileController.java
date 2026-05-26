package com.skillnet.web.controller;

import com.skillnet.config.MediaProperties;
import com.skillnet.service.media.MediaStorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media/files")
@CrossOrigin(origins = "*")
public class MediaFileController {

    private final MediaProperties mediaProperties;
    private final MediaStorageService mediaStorageService;

    public MediaFileController(MediaProperties mediaProperties, MediaStorageService mediaStorageService) {
        this.mediaProperties = mediaProperties;
        this.mediaStorageService = mediaStorageService;
    }

    @GetMapping("/**")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request) throws IOException {
        if (mediaProperties.isS3Enabled()) {
            return ResponseEntity.notFound().build();
        }

        String uri = request.getRequestURI();
        String marker = "/api/v1/media/files/";
        int idx = uri.indexOf(marker);
        if (idx < 0) {
            return ResponseEntity.notFound().build();
        }
        String key = uri.substring(idx + marker.length());

        if (key.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        Path file = mediaStorageService.resolveLocalPath(key);
        if (!Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(file);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
