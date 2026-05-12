package com.thecodinglab.imdbclone.media.internal;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioClientConfig {

  @Bean
  MinioClient minioClient(MediaStorageProperties properties) {
    return MinioClient.builder()
        .endpoint(properties.uri())
        .credentials(properties.accessKey(), properties.secretKey())
        .build();
  }
}
