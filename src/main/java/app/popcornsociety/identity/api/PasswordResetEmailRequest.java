package app.popcornsociety.identity.api;

import jakarta.validation.constraints.*;

public record PasswordResetEmailRequest(@NotBlank @Email @Size(max = 254) String email) {}
