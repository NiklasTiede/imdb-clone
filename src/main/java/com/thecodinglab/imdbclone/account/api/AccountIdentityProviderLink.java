package com.thecodinglab.imdbclone.account.api;

public record AccountIdentityProviderLink(
    Long accountId, String provider, String providerUserId, String email) {}
