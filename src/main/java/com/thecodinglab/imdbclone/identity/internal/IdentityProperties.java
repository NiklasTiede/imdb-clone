package com.thecodinglab.imdbclone.identity.internal;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "imdb-clone.identity")
public record IdentityProperties(
    @NotBlank String backendHost,
    @NotBlank String frontendHost,
    boolean emailVerificationEnabled) {}
