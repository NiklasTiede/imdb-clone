package com.thecodinglab.imdbclone.catalog.internal.search.query;

import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MovieSearchRankFusion {

  static final int RANK_CONSTANT = 10;

  public List<MovieSearchDocument> fuse(
      List<MovieSearchDocument> lexicalResults,
      List<MovieSearchDocument> semanticResults,
      int page,
      int size) {
    return fuse(lexicalResults, semanticResults, page, size, 0.5, 0.5);
  }

  public List<MovieSearchDocument> fuse(
      List<MovieSearchDocument> lexicalResults,
      List<MovieSearchDocument> semanticResults,
      int page,
      int size,
      double lexicalWeight,
      double semanticWeight) {
    validateWeights(lexicalWeight, semanticWeight);
    Map<Long, RankedMovie> rankedMovies = new LinkedHashMap<>();
    addRanks(rankedMovies, lexicalResults, lexicalWeight);
    addRanks(rankedMovies, semanticResults, semanticWeight);

    int from = page * size;
    return rankedMovies.values().stream()
        .sorted(
            Comparator.comparingDouble(RankedMovie::score)
                .reversed()
                .thenComparing(Comparator.comparingInt(RankedMovie::imdbRatingCount).reversed())
                .thenComparingInt(RankedMovie::firstSeenOrder))
        .skip(from)
        .limit(size)
        .map(RankedMovie::movie)
        .toList();
  }

  private void validateWeights(double lexicalWeight, double semanticWeight) {
    if (!Double.isFinite(lexicalWeight)
        || !Double.isFinite(semanticWeight)
        || lexicalWeight < 0
        || semanticWeight < 0
        || lexicalWeight + semanticWeight <= 0) {
      throw new IllegalArgumentException(
          "Search fusion weights must be finite, non-negative, and non-zero");
    }
  }

  private void addRanks(
      Map<Long, RankedMovie> rankedMovies, List<MovieSearchDocument> movies, double weight) {
    if (weight == 0) {
      return;
    }
    for (int index = 0; index < movies.size(); index++) {
      MovieSearchDocument movie = movies.get(index);
      long movieId = movie.getId();
      int rankPosition = index + 1;
      double rankScore = weight / (RANK_CONSTANT + rankPosition);
      int firstSeenOrder = rankedMovies.size();

      rankedMovies
          .computeIfAbsent(movieId, ignored -> new RankedMovie(movie, firstSeenOrder))
          .addScore(rankScore);
    }
  }

  private static class RankedMovie {

    private final MovieSearchDocument movie;
    private final int firstSeenOrder;
    private double score;

    RankedMovie(MovieSearchDocument movie, int firstSeenOrder) {
      this.movie = movie;
      this.firstSeenOrder = firstSeenOrder;
    }

    void addScore(double score) {
      this.score += score;
    }

    MovieSearchDocument movie() {
      return movie;
    }

    int firstSeenOrder() {
      return firstSeenOrder;
    }

    int imdbRatingCount() {
      return movie.getImdbRatingCount() == null ? 0 : movie.getImdbRatingCount();
    }

    double score() {
      return score;
    }
  }
}
