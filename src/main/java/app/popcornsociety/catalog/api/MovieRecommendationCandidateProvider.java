package app.popcornsociety.catalog.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("recommendation")
public interface MovieRecommendationCandidateProvider {

  MovieRecommendationCandidates findCandidates(Long movieId, int candidateLimit);
}
