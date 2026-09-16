package app.popcornsociety.recommendation.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.popcornsociety.catalog.api.MovieRecommendationCandidateProvider;
import app.popcornsociety.catalog.api.MovieRecommendationCandidates;
import app.popcornsociety.catalog.api.MovieRecord;
import app.popcornsociety.recommendation.api.MovieRecommendation;
import app.popcornsociety.recommendation.api.MovieRecommendationSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentBasedRecommendationsTest {

  @Mock private MovieRecommendationCandidateProvider candidateProvider;
  @Mock private ContentRecommendationRanker ranker;

  @Test
  void similarMovies_usesBoundedCandidateWindowAndVersionsTheStrategy() {
    MovieRecord anchor = movie(1L);
    MovieRecommendationCandidates candidates = new MovieRecommendationCandidates(anchor, List.of());
    List<MovieRecommendation> ranked = List.of();
    when(candidateProvider.findCandidates(1L, 48)).thenReturn(candidates);
    when(ranker.rank(anchor, List.of(), 12)).thenReturn(ranked);

    MovieRecommendationSet result = service().similarMovies(1L, 12);

    assertThat(result.strategy()).isEqualTo("content-v1");
    assertThat(result.items()).isSameAs(ranked);
    verify(candidateProvider).findCandidates(1L, 48);
  }

  @Test
  void similarMovies_keepsSmallRequestsOnMinimumCandidateWindow() {
    MovieRecord anchor = movie(1L);
    MovieRecommendationCandidates candidates = new MovieRecommendationCandidates(anchor, List.of());
    when(candidateProvider.findCandidates(1L, 24)).thenReturn(candidates);
    when(ranker.rank(anchor, List.of(), 3)).thenReturn(List.of());

    service().similarMovies(1L, 3);

    verify(candidateProvider).findCandidates(1L, 24);
  }

  private ContentBasedRecommendations service() {
    return new ContentBasedRecommendations(candidateProvider, ranker);
  }

  private MovieRecord movie(Long id) {
    return new MovieRecord(
        id, null, null, null, "Movie", "Movie", false, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null);
  }
}
