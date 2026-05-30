package com.skillnet.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(name = "skillnet.media.storage", havingValue = "s3")
public class AwsS3Configuration {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(AwsProperties awsProperties) {
        if (!awsProperties.hasStaticCredentials()) {
            throw new IllegalStateException(
                    "skillnet.media.storage=s3 requiere aws.access.key.id y aws.secret.access.key");
        }
        return S3Client.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        awsProperties.getAccessKeyId(), awsProperties.getSecretAccessKey())))
                .build();
    }
}
