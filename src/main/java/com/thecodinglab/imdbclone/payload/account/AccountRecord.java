package com.thecodinglab.imdbclone.payload.account;

import java.util.Date;
import jakarta.validation.constraints.*;

public record AccountRecord(
        @Size(min = 2, max = 50)
        String username,

        String password,

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
