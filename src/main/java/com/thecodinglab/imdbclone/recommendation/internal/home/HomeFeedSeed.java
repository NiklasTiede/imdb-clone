package com.thecodinglab.imdbclone.recommendation.internal.home;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class HomeFeedSeed {

  private HomeFeedSeed() {}

  static String canonical(String requestedSeed, String feedInstanceId) {
    if (requestedSeed != null && !requestedSeed.isBlank()) {
      return requestedSeed;
    }
    String source =
        feedInstanceId == null || feedInstanceId.isBlank()
            ? UUID.randomUUID().toString()
            : feedInstanceId.trim();
    return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
  }

  static long derive(String seed, String discriminator) {
    UUID uuid =
        UUID.nameUUIDFromBytes((seed + ":" + discriminator).getBytes(StandardCharsets.UTF_8));
    return uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
  }
}
