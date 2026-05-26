package com.skillnet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "skillnet.media")
public class MediaProperties {

    /** local | s3 */
    private String storage = "local";

    private String localDir = "uploads/media";

    /** Base URL for locally stored files (no trailing slash). */
    private String publicBaseUrl = "http://127.0.0.1:8080/api/v1/media/files";

    private String s3Bucket = "";

    private String s3Region = "us-east-1";

    private String s3AccessKey = "";

    private String s3SecretKey = "";

    /** CloudFront or bucket public URL (no trailing slash). */
    private String s3PublicBaseUrl = "";

    public boolean isS3Enabled() {
        return "s3".equalsIgnoreCase(storage)
                && s3Bucket != null
                && !s3Bucket.isBlank()
                && s3AccessKey != null
                && !s3AccessKey.isBlank()
                && s3SecretKey != null
                && !s3SecretKey.isBlank();
    }
}
