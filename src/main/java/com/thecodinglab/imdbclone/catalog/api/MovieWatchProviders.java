package com.thecodinglab.imdbclone.catalog.api;

import java.time.Instant;
import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public interface MovieWatchProviders {
  enum Outcome {
    AVAILABLE,
    NO_OFFERS,
    DISABLED,
    MOVIE_NOT_FOUND,
    UNMAPPED,
    UNSUPPORTED_TYPE,
    NOT_FOUND,
    IDENTITY_MISMATCH,
    RATE_LIMITED,
    UNAVAILABLE
  }

  record Offers(
      List<String> subscription,
      List<String> free,
      List<String> ads,
      List<String> rent,
      List<String> buy) {}

  record Result(
      String contractVersion,
      long movieId,
      String country,
      Outcome outcome,
      String source,
      String sourceUrl,
      Instant fetchedAt,
      Offers offers) {}

  Result get(long movieId, String country);
}
