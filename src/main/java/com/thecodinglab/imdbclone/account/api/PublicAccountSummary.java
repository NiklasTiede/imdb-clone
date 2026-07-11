package com.thecodinglab.imdbclone.account.api;

public record PublicAccountSummary(
    Long id, String username, String displayName, String imageUrlToken) {}
