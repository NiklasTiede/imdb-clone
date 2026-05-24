package com.thecodinglab.imdbclone.catalog.internal.search.query;

import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MovieSearchRankFusion {

  private static final int RANK_CONSTANT = 60;

  public List<MovieSearchDocument> fuse(
      List<MovieSearchDocument> lexicalResults,
      List<MovieSearchDocument> semanticResults,
      int page,
      int size) {
    Map<Long, RankedMovie> rankedMovies = new LinkedHashMap<>();
    addRanks(rankedMovies, lexicalResults);
    addRanks(rankedMovies, semanticResults);

    int from = page * size;
    return rankedMovies.values().stream()
        .sorted(
            Comparator.comparingDouble(RankedMovie::score)
                .reversed()
                .thenComparingInt(RankedMovie::firstSeenOrder))
        .skip(from)
        .limit(size)
        .map(RankedMovie::movie)
        .toList();
  }

  private void addRanks(Map<Long, RankedMovie> rankedMovies, List<MovieSearchDocument> movies) {
    for (int index = 0; index < movies.size(); index++) {
      MovieSearchDocument movie = movies.get(index);
      long movieId = movie.getId();
      int rankPosition = index + 1;
      double rankScore = 1.0 / (RANK_CONSTANT + rankPosition);
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

    double score() {
      return score;
    }
  }
}
