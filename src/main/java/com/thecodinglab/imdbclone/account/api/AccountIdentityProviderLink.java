package com.thecodinglab.imdbclone.account.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("identity")
public record AccountIdentityProviderLink(
    Long accountId, String provider, String providerUserId, String email) {}
