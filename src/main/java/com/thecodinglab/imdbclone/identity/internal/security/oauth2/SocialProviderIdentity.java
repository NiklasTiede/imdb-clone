package com.thecodinglab.imdbclone.identity.internal.security.oauth2;

import java.util.Map;

public record SocialProviderIdentity(
    String provider,
    String providerUserId,
    String email,
    boolean emailVerified,
    Map<String, Object> attributes) {}
