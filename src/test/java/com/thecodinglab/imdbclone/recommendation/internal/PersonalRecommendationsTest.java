package com.thecodinglab.imdbclone.recommendation.internal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.engagement.api.RatingPreferenceProvider;
import com.thecodinglab.imdbclone.recommendation.api.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PersonalRecommendationsTest {
  private final RatingPreferenceProvider preferences = mock(RatingPreferenceProvider.class);
  private final RecommendationService similar = mock(RecommendationService.class);
  private final PersonalRecommendations service = new PersonalRecommendations(preferences, similar);

  @Test
  void ranksSharedMatchesUsingActualRatingsAndExcludesTheEntirePersonalLibrary() {
    when(preferences.forAccount(7L))
        .thenReturn(
            new RatingPreferenceProvider.Preferences(
                List.of(
                    new RatingPreferenceProvider.Favorite(
                        1L, "Forrest Gump", new BigDecimal("9.0")),
                    new RatingPreferenceProvider.Favorite(2L, "Arrival", new BigDecimal("8.0"))),
                Set.of(1L, 2L, 3L, 4L),
                42));
    when(similar.similarMovies(1L, 30))
        .thenReturn(new MovieRecommendationSet("test", List.of(movie(3), movie(5), movie(6))));
    when(similar.similarMovies(2L, 30))
        .thenReturn(new MovieRecommendationSet("test", List.of(movie(4), movie(6), movie(7))));
    var result = service.recommend(7L, 2);
    assertThat(result.outcome()).isEqualTo(PersonalRecommendationService.Outcome.MATCHED);
    assertThat(result.items()).extracting(r -> r.movie().id()).containsExactly(6L, 5L);
    assertThat(result.items().getFirst().explanation())
        .contains("Forrest Gump 9.0/10", "Shared drama");
    assertThat(result.totalRatings()).isEqualTo(42);
    assertThat(result.basedOn())
        .extracting(PersonalRecommendationService.Basis::movieId)
        .containsExactly(1L, 2L);
  }

  @Test
  void noPositiveHistoryDoesNotPretendGenericPicksArePersonal() {
    when(preferences.forAccount(7L))
        .thenReturn(new RatingPreferenceProvider.Preferences(List.of(), Set.of(1L), 1));
    assertThat(service.recommend(7L, 3).outcome())
        .isEqualTo(PersonalRecommendationService.Outcome.NO_POSITIVE_RATINGS);
    verifyNoInteractions(similar);
  }

  @Test
  void noNewCandidatesIsExplicitAndLimitIsValidatedBeforeReadingData() {
    assertThatThrownBy(() -> service.recommend(7L, 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.recommend(7L, 11))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(preferences);
    when(preferences.forAccount(7L))
        .thenReturn(
            new RatingPreferenceProvider.Preferences(
                List.of(new RatingPreferenceProvider.Favorite(1L, "Forrest Gump", BigDecimal.TEN)),
                Set.of(1L, 3L),
                1));
    when(similar.similarMovies(1L, 30))
        .thenReturn(new MovieRecommendationSet("test", List.of(movie(3))));
    assertThat(service.recommend(7L, 3).outcome())
        .isEqualTo(PersonalRecommendationService.Outcome.NO_CANDIDATES);
  }

  private MovieRecommendation movie(long id) {
    return new MovieRecommendation(
        new MovieRecord(
            id, null, null, null, "Movie", "Movie", false, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null),
        RecommendationReason.SHARED_GENRES,
        "Shared drama");
  }
}
