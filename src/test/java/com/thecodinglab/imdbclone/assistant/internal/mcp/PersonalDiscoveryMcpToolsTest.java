package com.thecodinglab.imdbclone.assistant.internal.mcp;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.thecodinglab.imdbclone.engagement.api.AssistantRatingLibrary;
import com.thecodinglab.imdbclone.identity.api.ConciergeDelegation;
import com.thecodinglab.imdbclone.recommendation.api.PersonalRecommendationService;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.ai.mcp.annotation.McpMeta;
import org.springframework.security.access.AccessDeniedException;

class PersonalDiscoveryMcpToolsTest {
  private final ConciergeDelegation delegation = mock(ConciergeDelegation.class);
  private final AssistantRatingLibrary ratings = mock(AssistantRatingLibrary.class);
  private final PersonalRecommendationService recommendations =
      mock(PersonalRecommendationService.class);
  private final PersonalDiscoveryMcpTools tools =
      new PersonalDiscoveryMcpTools(
          new PersonalToolAuthorization(delegation), ratings, recommendations);
  private final McpMeta meta = mock(McpMeta.class);

  private void authorize() {
    when(meta.get("delegation")).thenReturn("synthetic");
    when(delegation.verify("synthetic", "ratings:read"))
        .thenReturn(new ConciergeDelegation.Actor(7L, "binding"));
  }

  @Test
  void rejectsUnauthorizedReadsBeforeEitherDomainCanBeCalled() {
    when(delegation.verify(null, "ratings:read")).thenThrow(new AccessDeniedException("Denied"));
    assertThatThrownBy(() -> tools.getMyRatings(meta, 0, AssistantRatingLibrary.Order.HIGHEST))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> tools.getMyRecommendations(meta, 3))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(ratings, recommendations);
  }

  @ParameterizedTest
  @EnumSource(AssistantRatingLibrary.Order.class)
  void readsOnlyTheDelegatedAccountAndKeepsPersonalScoresSeparate(
      AssistantRatingLibrary.Order order) {
    authorize();
    var movie = McpProtocolContractTest.CapturingMovieSearch.movie();
    var facet = new AssistantRatingLibrary.Facet("Drama", 1, BigDecimal.TEN);
    when(ratings.read(7L, 0, order))
        .thenReturn(
            new AssistantRatingLibrary.Result(
                new PagedResponse<>(
                    List.of(new AssistantRatingLibrary.Entry(movie, BigDecimal.TEN, Instant.EPOCH)),
                    0,
                    20,
                    21,
                    2,
                    false),
                BigDecimal.TEN,
                List.of(facet),
                List.of()));
    var result = tools.getMyRatings(meta, 0, order);
    assertThat(result.ratings().getFirst().userScore()).isEqualByComparingTo("10");
    assertThat(result.ratings().getFirst().movie()).isEqualTo(MovieToolMovie.from(movie));
    assertThat(result.totalElements()).isEqualTo(21);
    assertThat(result.last()).isFalse();
    assertThat(result.favoriteGenres()).containsExactly(facet);
    verify(ratings).read(7L, 0, order);
  }

  @Test
  void validatesBoundsAndPreservesInsufficientHistory() {
    authorize();
    for (int page : new int[] {-1, 101})
      assertThatThrownBy(() -> tools.getMyRatings(meta, page, AssistantRatingLibrary.Order.HIGHEST))
          .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> tools.getMyRatings(meta, 0, null))
        .isInstanceOf(IllegalArgumentException.class);
    for (int limit : new int[] {0, 11})
      assertThatThrownBy(() -> tools.getMyRecommendations(meta, limit))
          .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(ratings, recommendations);
    when(recommendations.recommend(7L, 3))
        .thenReturn(
            new PersonalRecommendationService.Result(
                "personal-ratings-v1",
                PersonalRecommendationService.Outcome.NO_POSITIVE_RATINGS,
                0,
                List.of(),
                List.of()));
    var result = tools.getMyRecommendations(meta, 3);
    assertThat(result.outcome())
        .isEqualTo(PersonalRecommendationService.Outcome.NO_POSITIVE_RATINGS);
    assertThat(result.movies()).isEmpty();
    verify(recommendations).recommend(7L, 3);
  }
}
