package com.thecodinglab.imdbclone.notification.internal;

public record NotificationMessage(Kind kind, String recipient, String username, String link) {
  public enum Kind {
    EMAIL_CONFIRMATION,
    PASSWORD_RESET
  }

  @Override
  public String toString() {
    return "NotificationMessage[kind=" + kind + ", content=redacted]";
  }
}
