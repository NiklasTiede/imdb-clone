package com.thecodinglab.imdbclone.catalog.internal.enrichment;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Ticker;
import com.thecodinglab.imdbclone.catalog.api.MovieReferenceService;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.catalog.api.MovieWatchProviders;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
class CatalogMovieWatchProviders implements MovieWatchProviders {
  private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());
  private final MovieReferenceService movies;
  private final TmdbClient client;
  private final Cache<Key, Result> cache;

  record Key(long movieId, long tmdbId, String imdbId, String country) {}

  @Autowired
  CatalogMovieWatchProviders(MovieReferenceService movies, TmdbClient client) {
    this(movies, client, Ticker.systemTicker());
  }

  CatalogMovieWatchProviders(MovieReferenceService movies, TmdbClient client, Ticker ticker) {
    this.movies = movies;
    this.client = client;
    cache =
        Caffeine.newBuilder()
            .maximumSize(1000)
            .ticker(ticker)
            .expireAfter(
                Expiry.<Key, Result>creating(
                    (key, value) ->
                        value.outcome() == Outcome.AVAILABLE || value.outcome() == Outcome.NO_OFFERS
                            ? Duration.ofMinutes(30)
                            : Duration.ofSeconds(30)))
            .build();
  }

  @Override
  public Result get(long movieId, String country) {
    if (movieId <= 0) throw new IllegalArgumentException("A positive catalog movie ID is required");
    String region = country == null ? "CH" : country.strip().toUpperCase(Locale.ROOT);
    if (!COUNTRIES.contains(region))
      throw new IllegalArgumentException("An ISO 3166-1 alpha-2 country is required");
    var found = movies.findMoviesByIds(List.of(movieId));
    if (found.isEmpty()) return empty(movieId, region, Outcome.MOVIE_NOT_FOUND);
    var movie = found.getFirst();
    if (movie.movieType() != MovieType.MOVIE)
      return empty(movieId, region, Outcome.UNSUPPORTED_TYPE);
    if (movie.tmdbId() == null || movie.tmdbId() <= 0)
      return empty(movieId, region, Outcome.UNMAPPED);
    if (!client.enabled()) return empty(movieId, region, Outcome.DISABLED);
    return cache.get(new Key(movieId, movie.tmdbId(), movie.imdbId(), region), this::load);
  }

  private Result load(Key key) {
    var response = client.watchProviders(key.tmdbId());
    if (response.outcome()
        != com.thecodinglab.imdbclone.catalog.api.MovieEnrichment.Outcome.AVAILABLE)
      return empty(key.movieId(), key.country(), Outcome.valueOf(response.outcome().name()));
    var results = response.data().path("results");
    if (!results.isObject()) return empty(key.movieId(), key.country(), Outcome.UNAVAILABLE);
    var region = results.path(key.country());
    if (!region.isMissingNode() && !region.isObject())
      return empty(key.movieId(), key.country(), Outcome.UNAVAILABLE);
    try {
      var offers =
          new Offers(
              providers(region, "flatrate"),
              providers(region, "free"),
              providers(region, "ads"),
              providers(region, "rent"),
              providers(region, "buy"));
      boolean any =
          !offers.subscription().isEmpty()
              || !offers.free().isEmpty()
              || !offers.ads().isEmpty()
              || !offers.rent().isEmpty()
              || !offers.buy().isEmpty();
      return new Result(
          "1.0",
          key.movieId(),
          key.country(),
          any ? Outcome.AVAILABLE : Outcome.NO_OFFERS,
          "JUSTWATCH_VIA_TMDB",
          "https://www.themoviedb.org/movie/" + key.tmdbId() + "/watch?locale=" + key.country(),
          Instant.now(),
          offers);
    } catch (IllegalArgumentException ignored) {
      return empty(key.movieId(), key.country(), Outcome.UNAVAILABLE);
    }
  }

  private static List<String> providers(JsonNode region, String category) {
    JsonNode entries = region.path(category);
    if (entries.isMissingNode()) return List.of();
    if (!entries.isArray()) throw new IllegalArgumentException("Malformed provider category");
    var names = new ArrayList<String>();
    for (JsonNode entry : entries) {
      var name = entry.path("provider_name");
      if (!name.isTextual() || name.asText().isBlank())
        throw new IllegalArgumentException("Malformed provider name");
      String value = name.asText().strip();
      value = value.substring(0, Math.min(120, value.length()));
      if (!names.contains(value)) names.add(value);
      if (names.size() == 12) break;
    }
    return List.copyOf(names);
  }

  private static Result empty(long movieId, String country, Outcome outcome) {
    return new Result("1.0", movieId, country, outcome, "JUSTWATCH_VIA_TMDB", null, null, null);
  }
}
