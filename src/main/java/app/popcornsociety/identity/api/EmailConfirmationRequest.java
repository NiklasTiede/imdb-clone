package app.popcornsociety.identity.api;

import jakarta.validation.constraints.*;

public record EmailConfirmationRequest(@NotBlank @Size(max = 512) String token) {}
