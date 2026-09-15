package com.thecodinglab.imdbclone.recommendation.api;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public interface PersonalRecommendationService {
  enum Outcome {
    MATCHED,
    NO_POSITIVE_RATINGS,
    NO_CANDIDATES
  }

  record Basis(Long movieId, String title, BigDecimal userScore) {}

  record Result(
      String strategy,
      Outcome outcome,
      long totalRatings,
      List<Basis> basedOn,
      List<MovieRecommendation> items) {}

  Result recommend(Long accountId, int limit);
}
