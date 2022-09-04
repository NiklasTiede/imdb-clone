package com.example.demo.Payload;

import java.util.Date;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;

public record AccountRecord(
    @NotBlank(message = "username is mandatory") String username,
    String password,
    String email,
    @Size(min = 2, max = 50, message = "firstName must be 2-35 characters long.") String firstName,
    @Size(min = 2, max = 50, message = "lastName must be 2-35 characters long.") String lastName,
    String phone,
    String bio,
    @Past(message = "Date input is invalid for a birth date.") Date birthday) {}
