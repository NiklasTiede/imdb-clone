package com.thecodinglab.imdbclone.identity.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "imdb-clone.identity")
public record IdentityProperties(
    @NotBlank String backendHost,
    @NotBlank String frontendHost,
    boolean emailVerificationEnabled,
    @Valid Webauthn webauthn) {

  public record Webauthn(@NotBlank String rpId, List<@NotBlank String> allowedOrigins) {}
}
