package com.thecodinglab.imdbclone.payload.account;

public record AccountSummaryResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName
) {}
