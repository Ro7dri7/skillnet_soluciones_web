package com.skillnet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Credenciales y región AWS (S3). Propiedades: {@code aws.region}, {@code aws.access.key.id},
 * {@code aws.secret.access.key}, {@code aws.s3.bucket.name}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {

    private String region = "us-east-1";

    private Access access = new Access();

    private Secret secret = new Secret();

    private S3 s3 = new S3();

    public String getAccessKeyId() {
        return access != null && access.getKey() != null ? nullToEmpty(access.getKey().getId()) : "";
    }

    public String getSecretAccessKey() {
        return secret != null && secret.getAccess() != null ? nullToEmpty(secret.getAccess().getKey()) : "";
    }

    public String getBucketName() {
        return s3 != null && s3.getBucket() != null ? nullToEmpty(s3.getBucket().getName()) : "";
    }

    public boolean hasStaticCredentials() {
        return !getAccessKeyId().isBlank() && !getSecretAccessKey().isBlank();
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    @Getter
    @Setter
    public static class Access {
        private Key key = new Key();
    }

    @Getter
    @Setter
    public static class Key {
        private String id;
    }

    @Getter
    @Setter
    public static class Secret {
        private SecretAccess access = new SecretAccess();
    }

    /** Mapea {@code aws.secret.access.key}. */
    @Getter
    @Setter
    public static class SecretAccess {
        private String key;
    }

    @Getter
    @Setter
    public static class S3 {
        private Bucket bucket = new Bucket();
    }

    @Getter
    @Setter
    public static class Bucket {
        private String name;
    }
}
