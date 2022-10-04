package com.thecodinglab.imdbclone.payload;

import com.thecodinglab.imdbclone.util.ValidatePassword;

import java.util.Date;
import javax.validation.constraints.Email;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;

public record AccountRecord(

        @Size(min = 2, max = 50, message = "firstName must be 2-50 characters long.")
        String username,

        @ValidatePassword
        String password,

        @Email(message = "Email must be valid.")
        String email,

        @Size(min = 2, max = 35, message = "firstName must be 2-35 characters long.")
        String firstName,

        @Size(min = 2, max = 35, message = "lastName must be 2-35 characters long.")
        String lastName,

        String phone,
        String bio,

        @Past(message = "Date input is invalid for a birth date.")
        Date birthday
) {}
