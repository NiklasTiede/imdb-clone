package com.example.demo.payload;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public record RegistrationRequest(

        @NotBlank
        @Size(min = 2, max = 50, message = "username must be 2-50 characters long.")
        String username,

        @NotBlank
        @Email(message = "Email must be valid.")
        String email,

        @NotBlank
        @Size(min = 6, max = 20)
        String password
) {}
