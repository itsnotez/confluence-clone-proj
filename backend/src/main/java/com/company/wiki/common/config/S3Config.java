package com.company.wiki.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${storage.s3.endpoint}") String endpoint;
    @Value("${storage.s3.access-key}") String accessKey;
    @Value("${storage.s3.secret-key}") String secretKey;
    @Value("${storage.s3.region}") String region;
    @Value("${storage.s3.bucket}") String bucket;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Bean
    ApplicationRunner ensureBucket(S3Client s3) {
        return args -> {
            try {
                s3.headBucket(r -> r.bucket(bucket));
            } catch (NoSuchBucketException e) {
                s3.createBucket(r -> r.bucket(bucket));
            }
        };
    }
}
