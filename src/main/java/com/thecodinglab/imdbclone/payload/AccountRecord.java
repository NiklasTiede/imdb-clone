package com.thecodinglab.imdbclone.payload;

import java.util.Date;
import javax.validation.constraints.Email;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;

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
