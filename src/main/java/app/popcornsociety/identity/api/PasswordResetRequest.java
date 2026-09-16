package app.popcornsociety.identity.api;

import app.popcornsociety.shared.validation.ValidPassword;
import jakarta.validation.constraints.*;

public record PasswordResetRequest(
    @NotBlank @Size(min = 32, max = 128) String token, @ValidPassword String newPassword) {}
