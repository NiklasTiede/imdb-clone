package com.thecodinglab.imdbclone.payload;

import com.thecodinglab.imdbclone.validation.ValidPassword;

import javax.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank
        String usernameOrEmail,

        @NotBlank
        @ValidPassword
        String password
) {}
