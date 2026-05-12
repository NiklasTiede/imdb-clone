package com.thecodinglab.imdbclone.engagement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.engagement.internal.RatingAggregateScheduler;
import com.thecodinglab.imdbclone.engagement.internal.persistence.Rating;
import com.thecodinglab.imdbclone.engagement.internal.persistence.RatingRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RatingAggregateSchedulerTest {

  @Mock private RatingRepository ratingRepository;

  @Mock private MovieService movieService;

  @InjectMocks private RatingAggregateScheduler scheduler;

  @Test
  void updateMovieRatings_withNoRecentRatingsDoesNothing() {
    when(ratingRepository.findAllByModifiedAtInUtcAfter(any())).thenReturn(List.of());

    scheduler.updateMovieRatings();

    verify(movieService, never()).updateRatingAggregate(any(), any(), anyInt());
  }

  @Test
  void updateMovieRatings_updatesEachMovieOnceWithAverageAndCount() {
    when(ratingRepository.findAllByModifiedAtInUtcAfter(any()))
        .thenReturn(
            List.of(
                Rating.create(new BigDecimal("7.0"), 1L, 1L),
                Rating.create(new BigDecimal("9.0"), 1L, 2L)));
    when(ratingRepository.findRatingsByIdMovieId(1L))
        .thenReturn(
            List.of(
                Rating.create(new BigDecimal("7.0"), 1L, 1L),
                Rating.create(new BigDecimal("9.0"), 1L, 2L),
                Rating.create(new BigDecimal("10.0"), 1L, 3L)));
    when(movieService.updateRatingAggregate(1L, new BigDecimal("8.7"), 3))
        .thenReturn(movieRecord(1L, "testMovieOnePri", 8.7F, 3));

    scheduler.updateMovieRatings();

    verify(movieService).updateRatingAggregate(1L, new BigDecimal("8.7"), 3);
  }

  private MovieRecord movieRecord(Long id, String primaryTitle, Float rating, Integer ratingCount) {
    return new MovieRecord(
        id,
        primaryTitle,
        primaryTitle,
        2010,
        null,
        100,
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        rating,
        ratingCount,
        null,
        null);
  }
}
