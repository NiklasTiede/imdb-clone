package com.thecodinglab.imdbclone.catalog.internal.enrichment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.thecodinglab.imdbclone.catalog.api.*;
import com.thecodinglab.imdbclone.catalog.api.MovieEnrichment.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogMovieEnrichmentTest {
  private final MovieReferenceService movies = mock(MovieReferenceService.class);
  private final TmdbClient client = mock(TmdbClient.class);
  private final MovieRecord movie = mock(MovieRecord.class);
  private final AtomicLong nanos = new AtomicLong();
  private final CatalogMovieEnrichment service =
      new CatalogMovieEnrichment(movies, client, nanos::get);
  private final Facts facts =
      new Facts(
          null,
          null,
          List.of(),
          List.of("Director"),
          List.of(),
          List.of(),
          List.of(),
          List.of(),
          null,
          null);

  @BeforeEach
  void setup() {
    when(movies.findMoviesByIds(List.of(6L))).thenReturn(List.of(movie));
    when(movie.movieType()).thenReturn(MovieType.MOVIE);
    when(movie.tmdbId()).thenReturn(13L);
    when(movie.imdbId()).thenReturn("tt0109830");
    when(client.enabled()).thenReturn(true);
  }

  @Test
  void resolvesOnlyCatalogMappingsAndHandlesUnsupportedOrMissingMovies() {
    assertThatThrownBy(() -> service.get(0)).isInstanceOf(IllegalArgumentException.class);
    assertThat(service.get(99).outcome()).isEqualTo(Outcome.MOVIE_NOT_FOUND);
    when(movie.movieType()).thenReturn(MovieType.TV_SERIES);
    assertThat(service.get(6).outcome()).isEqualTo(Outcome.UNSUPPORTED_TYPE);
    when(movie.movieType()).thenReturn(MovieType.MOVIE);
    when(movie.tmdbId()).thenReturn(null);
    assertThat(service.get(6).outcome()).isEqualTo(Outcome.UNMAPPED);
    when(movie.tmdbId()).thenReturn(13L);
    when(client.enabled()).thenReturn(false);
    assertThat(service.get(6).outcome()).isEqualTo(Outcome.DISABLED);
    verify(client, never()).fetch(anyLong(), any());
  }

  @Test
  void cachesSuccessAndReturnsDatedStaleDataOnlyDuringTransientFailure() {
    when(client.fetch(13, "tt0109830"))
        .thenReturn(
            new TmdbClient.Response(Outcome.AVAILABLE, facts),
            new TmdbClient.Response(Outcome.UNAVAILABLE, null));
    var first = service.get(6);
    assertThat(first.movieId()).isEqualTo(6);
    assertThat(first.sourceUrl()).isEqualTo("https://www.themoviedb.org/movie/13");
    assertThat(first.fetchedAt()).isNotNull();
    assertThat(service.get(6)).isEqualTo(first);
    verify(client, times(1)).fetch(13, "tt0109830");
    nanos.addAndGet(Duration.ofHours(7).toNanos());
    var stale = service.get(6);
    assertThat(stale.outcome()).isEqualTo(Outcome.STALE);
    assertThat(stale.facts()).isEqualTo(first.facts());
    assertThat(stale.fetchedAt()).isEqualTo(first.fetchedAt());
    nanos.addAndGet(Duration.ofHours(18).toNanos());
    assertThat(service.get(6).outcome()).isEqualTo(Outcome.UNAVAILABLE);
  }

  @Test
  void failuresHaveShortCacheAndIdentityChangesNeverReusePreviousFacts() {
    when(client.fetch(13, "tt0109830"))
        .thenReturn(
            new TmdbClient.Response(Outcome.UNAVAILABLE, null),
            new TmdbClient.Response(Outcome.AVAILABLE, facts));
    assertThat(service.get(6).outcome()).isEqualTo(Outcome.UNAVAILABLE);
    assertThat(service.get(6).outcome()).isEqualTo(Outcome.UNAVAILABLE);
    verify(client, times(1)).fetch(13, "tt0109830");
    nanos.addAndGet(Duration.ofSeconds(31).toNanos());
    assertThat(service.get(6).outcome()).isEqualTo(Outcome.AVAILABLE);
    when(movie.imdbId()).thenReturn("tt9999999");
    when(client.fetch(13, "tt9999999"))
        .thenReturn(new TmdbClient.Response(Outcome.IDENTITY_MISMATCH, null));
    var mismatch = service.get(6);
    assertThat(mismatch.outcome()).isEqualTo(Outcome.IDENTITY_MISMATCH);
    assertThat(mismatch.facts()).isNull();
  }

  @Test
  void removedProviderEntryDoesNotServeStaleFacts() {
    when(client.fetch(13, "tt0109830"))
        .thenReturn(
            new TmdbClient.Response(Outcome.AVAILABLE, facts),
            new TmdbClient.Response(Outcome.NOT_FOUND, null));
    service.get(6);
    nanos.addAndGet(Duration.ofHours(7).toNanos());
    assertThat(service.get(6).outcome()).isEqualTo(Outcome.NOT_FOUND);
    assertThat(service.get(6).facts()).isNull();
  }
}
