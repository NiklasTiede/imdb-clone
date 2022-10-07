package com.thecodinglab.imdbclone.payload;

import com.thecodinglab.imdbclone.validation.ValidPassword;

import java.util.Date;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;

public record AccountRecord(

        @NotBlank
        @Size(min = 2, max = 50)
        String username,

        @NotBlank
        @ValidPassword
        String password,

        @NotBlank
        @Email
        String email,

        @Size(min = 2, max = 35)
        String firstName,

        @Size(min = 2, max = 35)
        String lastName,

        String phone,
        String bio,

        @Past
        Date birthday
) {}
