package com.thecodinglab.imdbclone.payload;

import com.thecodinglab.imdbclone.validation.ValidPassword;

import javax.validation.constraints.Size;

public record PasswordResetRequest(

        @Size(min = 36, max = 36)
        String token,

        @ValidPassword
        String newPassword
) {}
