package com.skillnet.web.controller;

import com.skillnet.config.AwsProperties;
import com.skillnet.config.MediaProperties;
import com.skillnet.service.media.MediaStorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

@RestController
@RequestMapping("/api/v1/media/files")
@CrossOrigin(origins = "*")
public class MediaFileController {

    private final MediaProperties mediaProperties;
    private final MediaStorageService mediaStorageService;
    private final ObjectProvider<S3Client> s3ClientProvider;
    private final AwsProperties awsProperties;

    public MediaFileController(
            MediaProperties mediaProperties,
            MediaStorageService mediaStorageService,
            ObjectProvider<S3Client> s3ClientProvider,
            AwsProperties awsProperties) {
        this.mediaProperties = mediaProperties;
        this.mediaStorageService = mediaStorageService;
        this.s3ClientProvider = s3ClientProvider;
        this.awsProperties = awsProperties;
    }

    @GetMapping("/**")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request) throws IOException {
        String key = extractStorageKey(request.getRequestURI());
        if (key == null || key.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        if (mediaProperties.isS3Enabled()) {
            return serveFromS3(key);
        }
        return serveFromLocal(key);
    }

    private ResponseEntity<Resource> serveFromLocal(String key) throws IOException {
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
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private ResponseEntity<Resource> serveFromS3(String key) {
        S3Client client = s3ClientProvider.getIfAvailable();
        if (client == null) {
            return ResponseEntity.notFound().build();
        }

        String bucket = awsProperties.getBucketName();
        if (bucket == null || bucket.isBlank()) {
            bucket = mediaProperties.getS3Bucket();
        }
        if (bucket == null || bucket.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder().bucket(bucket).key(key).build();
            var headResponse = client.headObject(headRequest);
            String contentType = headResponse.contentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            ResponseInputStream<GetObjectResponse> objectStream =
                    client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());

            InputStreamResource resource = new InputStreamResource(objectStream) {
                @Override
                public long contentLength() {
                    return headResponse.contentLength() != null ? headResponse.contentLength() : -1;
                }
            };

            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }

    private String extractStorageKey(String uri) {
        String marker = MediaStorageService.MEDIA_FILES_PREFIX;
        int idx = uri.indexOf(marker);
        if (idx < 0) {
            marker = "/media/files/";
            idx = uri.indexOf(marker);
            if (idx < 0) {
                return null;
            }
            return uri.substring(idx + marker.length());
        }
        return uri.substring(idx + marker.length());
    }
}
