package com.thecodinglab.imdbclone.media.internal;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class ObjectStorageClientConfig {

  @Bean
  S3Client s3Client(MediaStorageProperties properties) {
    return S3Client.builder()
        .endpointOverride(URI.create(properties.uri()))
        .region(Region.US_EAST_1)
        .credentialsProvider(credentialsProvider(properties))
        .forcePathStyle(true)
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .build();
  }

  @Bean
  S3Presigner s3Presigner(MediaStorageProperties properties) {
    return S3Presigner.builder()
        .endpointOverride(URI.create(properties.uri()))
        .region(Region.US_EAST_1)
        .credentialsProvider(credentialsProvider(properties))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

  private StaticCredentialsProvider credentialsProvider(MediaStorageProperties properties) {
    return StaticCredentialsProvider.create(
        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
  }
}
