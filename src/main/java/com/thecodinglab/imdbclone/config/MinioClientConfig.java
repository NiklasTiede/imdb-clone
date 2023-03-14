package com.thecodinglab.imdbclone.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioClientConfig {

  @Value("${minio.rest.uri}")
  public String minioUrl;

  @Value("${minio.rest.accessKey}")
  public String accessKey;

  @Value("${minio.rest.secretKey}")
  public String secretKey;

  public MinioClient getClient() {
    return MinioClient.builder().endpoint(minioUrl).credentials(accessKey, secretKey).build();
  }
}
