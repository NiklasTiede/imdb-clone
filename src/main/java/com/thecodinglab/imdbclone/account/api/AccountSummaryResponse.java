package com.thecodinglab.imdbclone.account.api;

public record AccountSummaryResponse(
    Long id, String username, String email, String firstName, String lastName) {}
