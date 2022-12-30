package com.thecodinglab.imdbclone.payload;

import com.thecodinglab.imdbclone.validation.ValidPassword;

import jakarta.validation.constraints.*;


public record LoginRequest(

        @NotBlank
        String usernameOrEmail,

        @NotBlank
        @ValidPassword
        String password
) {}
