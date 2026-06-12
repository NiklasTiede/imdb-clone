package com.thecodinglab.imdbclone.identity.web;

import java.time.Instant;

public record PasskeyCredentialResponse(
    String credentialId, String label, Instant createdAt, Instant lastUsedAt) {}
