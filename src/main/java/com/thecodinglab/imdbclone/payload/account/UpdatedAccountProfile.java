package com.thecodinglab.imdbclone.payload.account;

import java.time.LocalDate;

public record UpdatedAccountProfile(
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        String bio,
        LocalDate birthday
) {}
