package com.thecodinglab.imdbclone.identity.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "imdb-clone.identity")
public record IdentityProperties(
    @NotBlank String backendHost,
    @NotBlank String frontendHost,
    boolean emailVerificationEnabled,
    @Valid @NotNull Jwt jwt,
    @Valid @NotNull Cors cors) {

  public record Jwt(@NotBlank String secret, @Positive long expirationInMs) {}

  public record Cors(@NotEmpty List<@NotBlank String> allowedOrigins) {}
}
