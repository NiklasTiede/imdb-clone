package com.thecodinglab.imdbclone.payload.account;

import java.time.LocalDate;

public record AccountProfile(
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        String bio,
        LocalDate birthday,
        String imageUrlToken,
        Long ratingsCount,
        Long watchlistCount,
        Long commentsCount
) {}
