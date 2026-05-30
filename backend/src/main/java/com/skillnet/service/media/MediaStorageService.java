package com.skillnet.service.media;

import com.skillnet.config.AwsProperties;
import com.skillnet.config.MediaProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class MediaStorageService {

    private final MediaProperties properties;
    private final AwsProperties awsProperties;
    private final ObjectProvider<S3Client> s3ClientProvider;

    public MediaStorageService(
            MediaProperties properties,
            AwsProperties awsProperties,
            ObjectProvider<S3Client> s3ClientProvider) {
        this.properties = properties;
        this.awsProperties = awsProperties;
        this.s3ClientProvider = s3ClientProvider;
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
        S3Client client = s3ClientProvider.getIfAvailable();
        if (client == null) {
            throw new IllegalStateException("S3Client no disponible; verifica skillnet.media.storage=s3 y credenciales AWS.");
        }

        String bucket = resolveS3Bucket();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build();
        client.putObject(request, RequestBody.fromInputStream(input, size));

        String region = awsProperties.getRegion();
        if (region == null || region.isBlank()) {
            region = properties.getS3Region();
        }
        String publicBase = properties.getS3PublicBaseUrl();
        if (publicBase == null || publicBase.isBlank()) {
            publicBase = "https://" + bucket + ".s3." + region + ".amazonaws.com";
        }
        String publicUrl = publicBase.replaceAll("/$", "") + "/" + storageKey;
        return new StoredMedia(storageKey, publicUrl);
    }

    private String resolveS3Bucket() {
        String bucket = awsProperties.getBucketName();
        if (bucket == null || bucket.isBlank()) {
            bucket = properties.getS3Bucket();
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Bucket S3 no configurado (aws.s3.bucket.name).");
        }
        return bucket;
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
