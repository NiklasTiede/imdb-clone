package app.popcornsociety.identity.api;

import app.popcornsociety.shared.validation.ValidPassword;
import jakarta.validation.constraints.*;

public record LoginRequest(
    @NotBlank String usernameOrEmail, @NotBlank @ValidPassword String password) {}
