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

    public static final String MEDIA_FILES_PREFIX = "/api/v1/media/files/";

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
        // Ruta relativa: el front (o nginx) proxyea /api/v1/media → backend (como Lernymart /media/).
        return new StoredMedia(storageKey, mediaFilesPath(storageKey));
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

    public void validateResourceFile(String contentType, long size) {
        if (size <= 0 || size > 50 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo debe pesar menos de 50 MB.");
        }
        if (contentType == null) {
            return;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("application/pdf")
                && !lower.startsWith("video/")
                && !lower.startsWith("image/")
                && !lower.startsWith("audio/")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Formato no permitido. Usa PDF, imagen, audio o vídeo.");
        }
    }

    /** URL accesible por el cliente (relativa en local, absoluta en S3). */
    public String resolvePublicUrl(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        if (properties.isS3Enabled()) {
            String publicBase = properties.getS3PublicBaseUrl();
            if (publicBase == null || publicBase.isBlank()) {
                String bucket = resolveS3Bucket();
                String region = awsProperties.getRegion();
                if (region == null || region.isBlank()) {
                    region = properties.getS3Region();
                }
                publicBase = "https://" + bucket + ".s3." + region + ".amazonaws.com";
            }
            return publicBase.replaceAll("/$", "") + "/" + storageKey;
        }
        return mediaFilesPath(storageKey);
    }

    public String mediaFilesPath(String storageKey) {
        return MEDIA_FILES_PREFIX + storageKey;
    }

    /** Extrae la clave S3/local desde una URL absoluta, relativa o clave cruda. */
    public String extractStorageKey(String urlOrKey) {
        if (urlOrKey == null || urlOrKey.isBlank()) {
            return null;
        }
        String trimmed = urlOrKey.trim();
        if (!trimmed.contains("://") && trimmed.startsWith("courses/")) {
            return trimmed;
        }
        int markerIdx = trimmed.indexOf(MEDIA_FILES_PREFIX);
        if (markerIdx >= 0) {
            return trimmed.substring(markerIdx + MEDIA_FILES_PREFIX.length());
        }
        String legacyMarker = "/media/files/";
        int legacyIdx = trimmed.indexOf(legacyMarker);
        if (legacyIdx >= 0) {
            return trimmed.substring(legacyIdx + legacyMarker.length());
        }
        return null;
    }

    /**
     * Normaliza URLs guardadas en BD (p. ej. http://127.0.0.1:8080/...) a rutas relativas en local
     * o URL pública de S3, para que estudiantes no dependan del host del infoproductor.
     */
    public String resolveMediaAccessUrl(String urlOrKey) {
        if (urlOrKey == null || urlOrKey.isBlank()) {
            return null;
        }
        String trimmed = urlOrKey.trim();
        String key = extractStorageKey(trimmed);
        if (key != null) {
            return resolvePublicUrl(key);
        }
        return trimmed;
    }

    public String resolveCourseImageUrl(String imageUrl, String imageFile) {
        if (imageFile != null && !imageFile.isBlank()) {
            return resolvePublicUrl(imageFile);
        }
        return resolveMediaAccessUrl(imageUrl);
    }

    public String resolveCourseVideoUrl(String videoUrl, String videoFile) {
        if (videoFile != null && !videoFile.isBlank()) {
            return resolvePublicUrl(videoFile);
        }
        return resolveMediaAccessUrl(videoUrl);
    }
}
