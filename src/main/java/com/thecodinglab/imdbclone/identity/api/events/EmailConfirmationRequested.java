package com.thecodinglab.imdbclone.identity.api.events;

import java.time.Instant;

public record EmailConfirmationRequested(
    String emailAddress, String username, String link, Instant expiresAt) {
  @Override
  public String toString() {
    return "EmailConfirmationRequested[redacted]";
  }
}
