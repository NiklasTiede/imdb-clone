package com.thecodinglab.imdbclone.recommendation.internal.home;

import com.thecodinglab.imdbclone.shared.error.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class HomeFeedCursor {

  private static final String PREFIX = "home-v1:";

  private HomeFeedCursor() {}

  static String encode(int offset) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString((PREFIX + offset).getBytes(StandardCharsets.UTF_8));
  }

  static int decode(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return 0;
    }
    try {
      String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      if (!decoded.startsWith(PREFIX)) {
        throw new BadRequestException("The home feed cursor is invalid.");
      }
      int offset = Integer.parseInt(decoded.substring(PREFIX.length()));
      if (offset < 0) {
        throw new BadRequestException("The home feed cursor is invalid.");
      }
      return offset;
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException("The home feed cursor is invalid.");
    }
  }
}
