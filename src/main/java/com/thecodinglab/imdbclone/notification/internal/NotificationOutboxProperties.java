package com.thecodinglab.imdbclone.notification.internal;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("imdb-clone.notification.outbox")
public record NotificationOutboxProperties(String activeKey, Map<String, String> keys) {
  public NotificationOutboxProperties {
    keys = keys == null ? Map.of() : Map.copyOf(keys);
  }

  @Override
  public String toString() {
    return "NotificationOutboxProperties[redacted]";
  }
}
