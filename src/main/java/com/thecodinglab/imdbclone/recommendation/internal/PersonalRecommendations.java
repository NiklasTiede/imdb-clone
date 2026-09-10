package com.thecodinglab.imdbclone.recommendation.internal;

import com.thecodinglab.imdbclone.engagement.api.RatingPreferenceProvider;
import com.thecodinglab.imdbclone.recommendation.api.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
class PersonalRecommendations implements PersonalRecommendationService {
  private final RatingPreferenceProvider preferences;
  private final RecommendationService similar;

  PersonalRecommendations(RatingPreferenceProvider preferences, RecommendationService similar) {
    this.preferences = preferences;
    this.similar = similar;
  }

  @Override
  public Result recommend(Long accountId, int limit) {
    if (limit < 1 || limit > 10) throw new IllegalArgumentException("Invalid recommendation limit");
    var taste = preferences.forAccount(accountId);
    var basis =
        taste.favorites().stream().map(f -> new Basis(f.movieId(), f.title(), f.score())).toList();
    Map<Long, Double> scores = new HashMap<>();
    Map<Long, MovieRecommendation> candidates = new HashMap<>();
    for (var favorite : taste.favorites()) {
      var matches = similar.similarMovies(favorite.movieId(), 30).items();
      for (int rank = 0; rank < matches.size(); rank++) {
        var match = matches.get(rank);
        Long id = match.movie().id();
        if (id == null
            || taste.excludedMovieIds().contains(id)
            || taste.favorites().stream().anyMatch(f -> f.movieId().equals(id))) continue;
        scores.merge(id, (favorite.score().doubleValue() - 5) / (rank + 1), Double::sum);
        candidates.putIfAbsent(
            id,
            new MovieRecommendation(
                match.movie(),
                match.reason(),
                "Because you rated "
                    + favorite.title()
                    + " "
                    + favorite.score().toPlainString()
                    + "/10. "
                    + match.explanation()));
      }
    }
    List<MovieRecommendation> items =
        scores.keySet().stream()
            .sorted(
                Comparator.<Long>comparingDouble(scores::get)
                    .reversed()
                    .thenComparingLong(Long::longValue))
            .limit(limit)
            .map(candidates::get)
            .toList();
    return new Result(
        "personal-ratings-v1",
        basis.isEmpty()
            ? Outcome.NO_POSITIVE_RATINGS
            : items.isEmpty() ? Outcome.NO_CANDIDATES : Outcome.MATCHED,
        taste.totalRatings(),
        basis,
        items);
  }
}
