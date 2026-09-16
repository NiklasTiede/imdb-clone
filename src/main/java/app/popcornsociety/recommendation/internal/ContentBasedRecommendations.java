package app.popcornsociety.recommendation.internal;

import app.popcornsociety.catalog.api.MovieRecommendationCandidateProvider;
import app.popcornsociety.catalog.api.MovieRecommendationCandidates;
import app.popcornsociety.recommendation.api.MovieRecommendationSet;
import app.popcornsociety.recommendation.api.RecommendationService;
import org.springframework.stereotype.Service;

@Service
public class ContentBasedRecommendations implements RecommendationService {

  static final String STRATEGY_VERSION = "content-v1";
  private static final int MINIMUM_CANDIDATE_WINDOW = 24;
  private static final int MAXIMUM_CANDIDATE_WINDOW = 90;

  private final MovieRecommendationCandidateProvider candidateProvider;
  private final ContentRecommendationRanker ranker;

  public ContentBasedRecommendations(
      MovieRecommendationCandidateProvider candidateProvider, ContentRecommendationRanker ranker) {
    this.candidateProvider = candidateProvider;
    this.ranker = ranker;
  }

  @Override
  public MovieRecommendationSet similarMovies(Long movieId, int limit) {
    int candidateLimit =
        Math.min(MAXIMUM_CANDIDATE_WINDOW, Math.max(MINIMUM_CANDIDATE_WINDOW, limit * 4));
    MovieRecommendationCandidates candidates =
        candidateProvider.findCandidates(movieId, candidateLimit);
    return new MovieRecommendationSet(
        STRATEGY_VERSION, ranker.rank(candidates.anchor(), candidates.candidates(), limit));
  }
}
