package app.popcornsociety.media.internal;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "popcorn-society.media.storage")
public record MediaStorageProperties(
    @NotBlank String uri,
    @NotBlank String accessKey,
    @NotBlank String secretKey,
    @NotBlank String bucketName) {}
