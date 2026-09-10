package com.thecodinglab.imdbclone.identity.api;

import java.time.Instant;

public record ConciergeDelegationResponse(String token, Instant expiresAt) {}
