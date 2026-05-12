package com.thecodinglab.imdbclone.media.internal;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "imdb-clone.media.storage")
public record MediaStorageProperties(
    @NotBlank String uri,
    @NotBlank String accessKey,
    @NotBlank String secretKey,
    @NotBlank String bucketName) {}
