package app.popcornsociety.identity.api;

import java.time.Instant;

public record ConciergeDelegationResponse(String token, Instant expiresAt) {}
