package com.example.demo.payload;

import javax.validation.constraints.Size;

public record PasswordResetRequest(

        @Size(min = 36, max = 36, message = "token must have a length of 36 characters.")
        String token,

        String newPassword
) {}
