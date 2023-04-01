package com.thecodinglab.imdbclone.payload.account;

import java.util.Date;

public record AccountProfile(
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        String bio,
        Date birthday,
        String imageUrlToken,
        Long ratingsCount,
        Long watchlistCount,
        Long commentsCount
) {}
