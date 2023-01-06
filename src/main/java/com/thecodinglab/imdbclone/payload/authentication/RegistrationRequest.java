package com.thecodinglab.imdbclone.payload.authentication;

import com.thecodinglab.imdbclone.validation.ValidPassword;
import jakarta.validation.constraints.*;

public record RegistrationRequest(
    @NotBlank
    @Size(min = 3, max = 50)
    String username,

    @NotBlank
    @Email
    String email,

    @NotBlank
    @ValidPassword
    String password
) {}
