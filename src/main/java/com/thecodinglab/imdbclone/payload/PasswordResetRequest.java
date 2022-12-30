package com.thecodinglab.imdbclone.payload;

import com.thecodinglab.imdbclone.validation.ValidPassword;

import jakarta.validation.constraints.*;


public record PasswordResetRequest(

        @Size(min = 36, max = 36)
        String token,

        @ValidPassword
        String newPassword
) {}
