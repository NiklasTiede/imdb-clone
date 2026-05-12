package com.thecodinglab.imdbclone.account.api;

public record PublicAccountProfile(
    String username,
    String firstName,
    String lastName,
    String bio,
    String imageUrlToken,
    Long ratingsCount,
    Long watchlistCount,
    Long commentsCount) {}
