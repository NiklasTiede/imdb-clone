package com.thecodinglab.imdbclone.catalog.api;

import java.time.Instant;
import java.util.List;
import org.springframework.modulith.NamedInterface;

/** Optional external facts; never a replacement for catalog identity or personal state. */
@NamedInterface("assistant")
public interface MovieEnrichment {
  enum Outcome {
    AVAILABLE,
    STALE,
    DISABLED,
    MOVIE_NOT_FOUND,
    UNMAPPED,
    UNSUPPORTED_TYPE,
    NOT_FOUND,
    IDENTITY_MISMATCH,
    RATE_LIMITED,
    UNAVAILABLE
  }

  record CastMember(String name, String character) {}

  record Facts(
      String tagline,
      String releaseDate,
      List<CastMember> cast,
      List<String> directors,
      List<String> writers,
      List<String> productionCountries,
      List<String> productionCompanies,
      List<String> spokenLanguages,
      Long budgetUsd,
      Long revenueUsd) {}

  record Result(
      String contractVersion,
      long movieId,
      Outcome outcome,
      String source,
      String sourceUrl,
      Instant fetchedAt,
      Facts facts) {}

  Result get(long movieId);
}
