package com.thecodinglab.imdbclone.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioClientConfig {

  @Value("${minio.rest.uri}")
  public String minioUrl;

  @Value("${minio.rest.access-key}")
  public String accessKey;

  @Value("${minio.rest.secret-key}")
  public String secretKey;

  public MinioClient getClient() {
    return MinioClient.builder().endpoint(minioUrl).credentials(accessKey, secretKey).build();
  }
}
