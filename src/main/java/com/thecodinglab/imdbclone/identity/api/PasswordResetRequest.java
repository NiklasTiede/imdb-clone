package com.thecodinglab.imdbclone.identity.api;

import com.thecodinglab.imdbclone.validation.ValidPassword;
import jakarta.validation.constraints.*;

public record PasswordResetRequest(
    @Size(min = 36, max = 36) String token, @ValidPassword String newPassword) {}
