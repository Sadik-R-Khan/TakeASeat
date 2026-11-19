package com.takeaseat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
public class AwsS3Config {

    @Bean
    @ConditionalOnProperty(name = "aws.s3.enabled", havingValue = "true")
    public S3Client s3Client(
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.access-key:}") String accessKey,
            @Value("${aws.s3.secret-key:}") String secretKey
    ) {
        S3ClientBuilder builder = S3Client.builder().region(Region.of(region));

        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
            );
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
