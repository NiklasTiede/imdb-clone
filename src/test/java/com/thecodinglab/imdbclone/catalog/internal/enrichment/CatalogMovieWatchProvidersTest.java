package com.thecodinglab.imdbclone.catalog.internal.enrichment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.thecodinglab.imdbclone.catalog.api.*;
import com.thecodinglab.imdbclone.catalog.api.MovieWatchProviders.Outcome;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CatalogMovieWatchProvidersTest {
  private final MovieReferenceService movies = mock(MovieReferenceService.class);
  private final TmdbClient client = mock(TmdbClient.class);
  private final MovieRecord movie = mock(MovieRecord.class);
  private final AtomicLong nanos = new AtomicLong();
  private final CatalogMovieWatchProviders service =
      new CatalogMovieWatchProviders(movies, client, nanos::get);

  @BeforeEach
  void setup() {
    when(movies.findMoviesByIds(List.of(6L))).thenReturn(List.of(movie));
    when(movie.movieType()).thenReturn(MovieType.MOVIE);
    when(movie.tmdbId()).thenReturn(13L);
    when(client.enabled()).thenReturn(true);
  }

  private void response(String json) throws Exception {
    when(client.watchProviders(13))
        .thenReturn(
            new TmdbClient.Payload(
                MovieEnrichment.Outcome.AVAILABLE, JsonMapper.builder().build().readTree(json)));
  }

  @Test
  void defaultsToSwitzerlandAndSeparatesCountriesCategoriesAndSourceLinks() throws Exception {
    response(
        """
        {"id":13,"results":{
          "CH":{"link":"https://attacker.invalid","flatrate":[{"provider_name":"Swiss Subscription"}],
            "free":[{"provider_name":"Free Service"}],"ads":[{"provider_name":"Ad Service"}],
            "rent":[{"provider_name":"Swiss Rental"},{"provider_name":"Swiss Rental"}],
            "buy":[{"provider_name":"Swiss Store"}]},
          "DE":{"flatrate":[{"provider_name":"German Subscription"}]}}}
        """);
    var swiss = service.get(6, null);
    assertThat(swiss.country()).isEqualTo("CH");
    assertThat(swiss.outcome()).isEqualTo(Outcome.AVAILABLE);
    assertThat(swiss.offers().subscription()).containsExactly("Swiss Subscription");
    assertThat(swiss.offers().rent()).containsExactly("Swiss Rental");
    assertThat(swiss.offers().buy()).containsExactly("Swiss Store");
    assertThat(swiss.offers().free()).containsExactly("Free Service");
    assertThat(swiss.offers().ads()).containsExactly("Ad Service");
    assertThat(swiss.source()).isEqualTo("JUSTWATCH_VIA_TMDB");
    assertThat(swiss.sourceUrl()).isEqualTo("https://www.themoviedb.org/movie/13/watch?locale=CH");
    assertThat(service.get(6, "ch")).isEqualTo(swiss);
    assertThat(service.get(6, "DE").offers().subscription()).containsExactly("German Subscription");
    assertThat(service.get(6, "AT").outcome()).isEqualTo(Outcome.NO_OFFERS);
    assertThat(service.get(6, "AT").fetchedAt()).isNotNull();
    verify(client, times(3)).watchProviders(13);
  }

  @Test
  void expiresOffersWithoutServingOldAvailabilityAndBrieflyCachesFailures() throws Exception {
    response("{\"id\":13,\"results\":{\"CH\":{\"rent\":[{\"provider_name\":\"Rental\"}]}}}");
    var first = service.get(6, "CH");
    nanos.addAndGet(Duration.ofMinutes(29).toNanos());
    assertThat(service.get(6, "CH")).isEqualTo(first);
    when(client.watchProviders(13))
        .thenReturn(new TmdbClient.Payload(MovieEnrichment.Outcome.RATE_LIMITED, null));
    nanos.addAndGet(Duration.ofMinutes(2).toNanos());
    var failed = service.get(6, "CH");
    assertThat(failed.outcome()).isEqualTo(Outcome.RATE_LIMITED);
    assertThat(failed.offers()).isNull();
    assertThat(failed.sourceUrl()).isNull();
    assertThat(service.get(6, "CH")).isEqualTo(failed);
    verify(client, times(2)).watchProviders(13);
    nanos.addAndGet(Duration.ofSeconds(31).toNanos());
    service.get(6, "CH");
    verify(client, times(3)).watchProviders(13);
  }

  @Test
  void validatesCountryAndCatalogBeforeNetworkAccess() {
    assertThatThrownBy(() -> service.get(0, "CH")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.get(6, "ZZ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.get(6, "../CH")).isInstanceOf(IllegalArgumentException.class);
    assertThat(service.get(99, "CH").outcome()).isEqualTo(Outcome.MOVIE_NOT_FOUND);
    when(movie.movieType()).thenReturn(MovieType.TV_SERIES);
    assertThat(service.get(6, "CH").outcome()).isEqualTo(Outcome.UNSUPPORTED_TYPE);
    when(movie.movieType()).thenReturn(MovieType.MOVIE);
    when(movie.tmdbId()).thenReturn(null);
    assertThat(service.get(6, "CH").outcome()).isEqualTo(Outcome.UNMAPPED);
    when(movie.tmdbId()).thenReturn(13L);
    when(client.enabled()).thenReturn(false);
    assertThat(service.get(6, "CH").outcome()).isEqualTo(Outcome.DISABLED);
    verify(client, never()).watchProviders(anyLong());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{\"results\":[]}",
        "{\"results\":{\"CH\":null}}",
        "{\"results\":{\"CH\":{\"rent\":{}}}}",
        "{\"results\":{\"CH\":{\"rent\":[{}]}}}"
      })
  void malformedDataMeansUnavailableRatherThanNoOffers(String body) throws Exception {
    response(body);
    assertThat(service.get(6, "CH").outcome()).isEqualTo(Outcome.UNAVAILABLE);
  }
}
