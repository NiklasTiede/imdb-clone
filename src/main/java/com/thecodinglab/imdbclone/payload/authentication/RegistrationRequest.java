package com.thecodinglab.imdbclone.payload.authentication;

import com.thecodinglab.imdbclone.validation.AvailableEmail;
import com.thecodinglab.imdbclone.validation.AvailableUsername;
import com.thecodinglab.imdbclone.validation.ValidPassword;
import com.thecodinglab.imdbclone.validation.ValidUsername;
import jakarta.validation.constraints.*;

public record RegistrationRequest(

    @NotBlank
    @ValidUsername
    @AvailableUsername
    String username,

    @NotBlank
    @Email
    @AvailableEmail
    String email,

    @NotBlank
    @ValidPassword
    String password
) {}
