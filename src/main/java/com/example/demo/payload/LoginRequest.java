package com.example.demo.payload;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public record LoginRequest(

        @Size(max = 50, message = "username or email must be not longer than 80 characters.")
        String usernameOrEmail,

        @NotBlank
        @Size(min = 6, max = 20)
        String password
) {}
