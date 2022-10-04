package com.thecodinglab.imdbclone.payload;

import com.thecodinglab.imdbclone.util.ValidatePassword;

import javax.validation.constraints.Size;

public record PasswordResetRequest(

        @Size(min = 36, max = 36, message = "token must have a length of 36 characters.")
        String token,

        @ValidatePassword
        String newPassword
) {}
