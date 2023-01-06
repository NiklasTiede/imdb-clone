package com.thecodinglab.imdbclone.payload.account;

import java.util.Date;

public record UpdatedAccountProfile(
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        String bio,
        Date birthday
) {}
