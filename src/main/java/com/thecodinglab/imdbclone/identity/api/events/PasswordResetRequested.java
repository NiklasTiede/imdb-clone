package com.thecodinglab.imdbclone.identity.api.events;

import java.time.Instant;

public record PasswordResetRequested(
    String emailAddress, String username, String link, Instant expiresAt) {
  @Override
  public String toString() {
    return "PasswordResetRequested[redacted]";
  }
}
