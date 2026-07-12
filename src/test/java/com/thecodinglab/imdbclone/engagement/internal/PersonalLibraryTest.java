package com.thecodinglab.imdbclone.engagement.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieReferenceService;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.engagement.api.RatingLibraryResponse;
import com.thecodinglab.imdbclone.engagement.api.RatingLibrarySort;
import com.thecodinglab.imdbclone.engagement.internal.persistence.Rating;
import com.thecodinglab.imdbclone.engagement.internal.persistence.RatingRepository;
import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovieRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonalLibraryTest {

  @Mock private RatingRepository ratingRepository;
  @Mock private WatchedMovieRepository watchedMovieRepository;
  @Mock private MovieReferenceService movieReferenceService;
  @Mock private LibraryInsightsMetrics libraryInsightsMetrics;

  @Test
  void ratingLibrary_calculatesInsightsAcrossAllRatingsBeforePagination() {
    List<Rating> ratings =
        IntStream.rangeClosed(1, 31)
            .mapToObj(index -> rating(index, index % 2 == 0 ? "8.5" : "6.0"))
            .toList();
    List<MovieRecord> movies =
        IntStream.rangeClosed(1, 31)
            .mapToObj(index -> movie(index, "Movie " + index, 1990 + index % 3))
            .toList();
    when(ratingRepository.findAllByIdAccountId(4L)).thenReturn(ratings);
    when(movieReferenceService.findMoviesByIds(any())).thenReturn(movies);

    PersonalLibrary library =
        new PersonalLibrary(
            ratingRepository,
            watchedMovieRepository,
            movieReferenceService,
            libraryInsightsMetrics);
    RatingLibraryResponse response =
        library.getRatingLibrary(4L, 0, 30, RatingLibrarySort.SCORE_DESC);

    assertThat(response.items().getTotalElements()).isEqualTo(31);
    assertThat(response.items().getContent()).hasSize(30);
    assertThat(response.insights().totalRatings()).isEqualTo(31);
    assertThat(response.insights().distribution())
        .extracting(bucket -> bucket.count())
        .containsExactly(0, 0, 16, 0, 15, 0);
    assertThat(response.insights().favoriteGenres()).isNotEmpty();
    verify(movieReferenceService).findMoviesByIds(any());
  }

  private Rating rating(long movieId, String score) {
    Rating rating = org.mockito.Mockito.mock(Rating.class);
    when(rating.getAccountId()).thenReturn(4L);
    when(rating.getMovieId()).thenReturn(movieId);
    when(rating.getRating()).thenReturn(new BigDecimal(score));
    when(rating.getCreatedAtInUtc()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
    return rating;
  }

  private MovieRecord movie(long id, String title, int year) {
    return new MovieRecord(
        id,
        "tt" + id,
        null,
        MovieType.MOVIE,
        title,
        title,
        false,
        year,
        null,
        100,
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        Set.of(MovieGenre.SCI_FI),
        7.5f,
        1000,
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
