package com.thecodinglab.imdbclone.identity.api;

import com.thecodinglab.imdbclone.shared.validation.ValidPassword;
import jakarta.validation.constraints.*;

public record LoginRequest(
    @NotBlank String usernameOrEmail, @NotBlank @ValidPassword String password) {}
