package com.thecodinglab.imdbclone.catalog.internal.enrichment;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Ticker;
import com.thecodinglab.imdbclone.catalog.api.MovieEnrichment;
import com.thecodinglab.imdbclone.catalog.api.MovieReferenceService;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
class CatalogMovieEnrichment implements MovieEnrichment {
  private final MovieReferenceService movies;
  private final TmdbClient client;
  private final Cache<Key, Result> cache;
  private final Cache<Key, Result> lastGood;

  record Key(long movieId, long tmdbId, String imdbId) {}

  @Autowired
  CatalogMovieEnrichment(MovieReferenceService movies, TmdbClient client) {
    this(movies, client, Ticker.systemTicker());
  }

  CatalogMovieEnrichment(MovieReferenceService movies, TmdbClient client, Ticker ticker) {
    this.movies = movies;
    this.client = client;
    cache =
        Caffeine.newBuilder()
            .maximumSize(1000)
            .ticker(ticker)
            .expireAfter(
                new Expiry<Key, Result>() {
                  @Override
                  public long expireAfterCreate(Key key, Result value, long now) {
                    return (value.outcome() == Outcome.AVAILABLE
                            ? Duration.ofHours(6)
                            : Duration.ofSeconds(30))
                        .toNanos();
                  }

                  @Override
                  public long expireAfterUpdate(Key key, Result value, long now, long remaining) {
                    return expireAfterCreate(key, value, now);
                  }

                  @Override
                  public long expireAfterRead(Key key, Result value, long now, long remaining) {
                    return remaining;
                  }
                })
            .build();
    lastGood =
        Caffeine.newBuilder()
            .maximumSize(1000)
            .ticker(ticker)
            .expireAfterWrite(Duration.ofHours(24))
            .build();
  }

  @Override
  public Result get(long movieId) {
    if (movieId <= 0) throw new IllegalArgumentException("A positive catalog movie ID is required");
    var found = movies.findMoviesByIds(List.of(movieId));
    if (found.isEmpty()) return empty(movieId, Outcome.MOVIE_NOT_FOUND);
    var movie = found.getFirst();
    if (movie.movieType() != MovieType.MOVIE) return empty(movieId, Outcome.UNSUPPORTED_TYPE);
    if (movie.tmdbId() == null || movie.tmdbId() <= 0) return empty(movieId, Outcome.UNMAPPED);
    if (!client.enabled()) return empty(movieId, Outcome.DISABLED);
    return cache.get(new Key(movieId, movie.tmdbId(), movie.imdbId()), this::load);
  }

  private Result load(Key key) {
    var response = client.fetch(key.tmdbId(), key.imdbId());
    if (response.outcome() == Outcome.AVAILABLE) {
      var result =
          new Result(
              "1.0",
              key.movieId(),
              Outcome.AVAILABLE,
              "TMDB",
              "https://www.themoviedb.org/movie/" + key.tmdbId(),
              Instant.now(),
              response.facts());
      lastGood.put(key, result);
      return result;
    }
    var previous = lastGood.getIfPresent(key);
    if (previous != null
        && (response.outcome() == Outcome.UNAVAILABLE
            || response.outcome() == Outcome.RATE_LIMITED)) {
      return new Result(
          "1.0",
          key.movieId(),
          Outcome.STALE,
          "TMDB",
          previous.sourceUrl(),
          previous.fetchedAt(),
          previous.facts());
    }
    lastGood.invalidate(key);
    return empty(key.movieId(), response.outcome());
  }

  private static Result empty(long movieId, Outcome outcome) {
    return new Result("1.0", movieId, outcome, "TMDB", null, null, null);
  }
}
