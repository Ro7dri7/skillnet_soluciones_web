package com.skillnet.service.media;

import com.skillnet.config.MediaProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class MediaStorageService {

    private final MediaProperties properties;
    private volatile S3Client s3Client;

    public MediaStorageService(MediaProperties properties) {
        this.properties = properties;
    }

    public StoredMedia storeCourseFile(
            Long courseId, String kind, String originalFilename, String contentType, InputStream input, long size)
            throws IOException {
        String safeName = sanitizeFilename(originalFilename);
        String storageKey = "courses/" + courseId + "/" + kind + "/" + UUID.randomUUID() + "-" + safeName;

        if (properties.isS3Enabled()) {
            return storeToS3(storageKey, contentType, input, size);
        }
        return storeLocally(storageKey, input);
    }

    public Path resolveLocalPath(String storageKey) {
        return Path.of(properties.getLocalDir()).resolve(storageKey.replace("/", java.io.File.separator));
    }

    private StoredMedia storeLocally(String storageKey, InputStream input) throws IOException {
        Path target = resolveLocalPath(storageKey);
        Files.createDirectories(target.getParent());
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        String base = properties.getPublicBaseUrl().replaceAll("/$", "");
        String publicUrl = base + "/" + storageKey;
        return new StoredMedia(storageKey, publicUrl);
    }

    private StoredMedia storeToS3(String storageKey, String contentType, InputStream input, long size) {
        S3Client client = s3Client();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getS3Bucket())
                .key(storageKey)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build();
        client.putObject(request, RequestBody.fromInputStream(input, size));

        String publicBase = properties.getS3PublicBaseUrl();
        if (publicBase == null || publicBase.isBlank()) {
            publicBase = "https://" + properties.getS3Bucket() + ".s3." + properties.getS3Region() + ".amazonaws.com";
        }
        String publicUrl = publicBase.replaceAll("/$", "") + "/" + storageKey;
        return new StoredMedia(storageKey, publicUrl);
    }

    private S3Client s3Client() {
        if (s3Client == null) {
            synchronized (this) {
                if (s3Client == null) {
                    s3Client = S3Client.builder()
                            .region(Region.of(properties.getS3Region()))
                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                    properties.getS3AccessKey(), properties.getS3SecretKey())))
                            .build();
                }
            }
        }
        return s3Client;
    }

    private static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "file.bin";
        }
        String base = Path.of(name).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        if (base.isBlank()) {
            return "file.bin";
        }
        return base.toLowerCase(Locale.ROOT);
    }

    public void validateCover(String contentType, long size) {
        if (size <= 0 || size > 10 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen debe pesar menos de 10 MB.");
        }
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permiten imágenes (.jpg, .png).");
        }
    }

    public void validatePromoVideo(String contentType, long size) {
        if (size <= 0 || size > 100 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El vídeo debe pesar menos de 100 MB.");
        }
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("video/")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Solo se permiten archivos de vídeo para el tráiler.");
        }
    }
}
