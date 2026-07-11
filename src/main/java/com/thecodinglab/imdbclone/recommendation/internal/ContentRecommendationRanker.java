package com.thecodinglab.imdbclone.recommendation.internal;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecommendationCandidate;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.recommendation.api.MovieRecommendation;
import com.thecodinglab.imdbclone.recommendation.api.RecommendationReason;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ContentRecommendationRanker {

  private static final double DIVERSITY_PENALTY = 0.15;

  public List<MovieRecommendation> rank(
      MovieRecord anchor, List<MovieRecommendationCandidate> candidates, int limit) {
    List<ScoredCandidate> remaining =
        candidates.stream()
            .filter(candidate -> candidate.movie() != null)
            .filter(candidate -> !anchor.id().equals(candidate.movie().id()))
            .map(candidate -> new ScoredCandidate(candidate, baseScore(anchor, candidate)))
            .toList();
    List<ScoredCandidate> selected = new ArrayList<>();

    while (selected.size() < limit && selected.size() < remaining.size()) {
      ScoredCandidate next =
          remaining.stream()
              .filter(candidate -> !selected.contains(candidate))
              .min(
                  Comparator.<ScoredCandidate>comparingDouble(
                          candidate -> -adjustedScore(candidate, selected))
                      .thenComparingInt(candidate -> candidate.candidate().semanticRank())
                      .thenComparing(
                          candidate -> candidate.candidate().movie().id(),
                          Comparator.nullsLast(Long::compareTo)))
              .orElseThrow();
      selected.add(next);
    }

    return selected.stream()
        .map(candidate -> toRecommendation(anchor, candidate.candidate().movie()))
        .toList();
  }

  private double baseScore(MovieRecord anchor, MovieRecommendationCandidate candidate) {
    MovieRecord movie = candidate.movie();
    double semanticScore = 1.0 / Math.sqrt(Math.max(1, candidate.semanticRank()));
    double genreScore = genreSimilarity(anchor.movieGenre(), movie.movieGenre());
    double typeScore =
        anchor.movieType() != null && anchor.movieType() == movie.movieType() ? 1.0 : 0.0;
    double yearScore = yearSimilarity(anchor.startYear(), movie.startYear());
    double qualityScore = qualityScore(movie.imdbRating(), movie.imdbRatingCount());
    return semanticScore * 0.60
        + genreScore * 0.22
        + typeScore * 0.08
        + yearScore * 0.05
        + qualityScore * 0.05;
  }

  private double adjustedScore(ScoredCandidate candidate, List<ScoredCandidate> selected) {
    double maximumProfileSimilarity =
        selected.stream()
            .mapToDouble(
                selectedCandidate ->
                    profileSimilarity(
                        candidate.candidate().movie(), selectedCandidate.candidate().movie()))
            .max()
            .orElse(0.0);
    return candidate.baseScore() - maximumProfileSimilarity * DIVERSITY_PENALTY;
  }

  private double profileSimilarity(MovieRecord left, MovieRecord right) {
    double genreSimilarity = genreSimilarity(left.movieGenre(), right.movieGenre());
    double sameType = left.movieType() != null && left.movieType() == right.movieType() ? 1.0 : 0.0;
    double sameDecade =
        left.startYear() != null
                && right.startYear() != null
                && left.startYear() / 10 == right.startYear() / 10
            ? 1.0
            : 0.0;
    return genreSimilarity * 0.70 + sameType * 0.10 + sameDecade * 0.20;
  }

  private MovieRecommendation toRecommendation(MovieRecord anchor, MovieRecord movie) {
    Set<MovieGenre> sharedGenres = genres(anchor.movieGenre());
    sharedGenres.retainAll(genres(movie.movieGenre()));
    if (!sharedGenres.isEmpty()) {
      MovieGenre genre =
          sharedGenres.stream().min(Comparator.comparingInt(Enum::ordinal)).orElseThrow();
      return new MovieRecommendation(
          movie, RecommendationReason.SHARED_GENRES, "More %s movies".formatted(label(genre)));
    }
    if (sameEra(anchor, movie)) {
      return new MovieRecommendation(
          movie,
          RecommendationReason.SAME_ERA,
          "More movies from the %ds".formatted((movie.startYear() / 10) * 10));
    }
    return new MovieRecommendation(
        movie, RecommendationReason.SIMILAR_THEMES, "Similar themes and story elements");
  }

  private boolean sameEra(MovieRecord anchor, MovieRecord movie) {
    return anchor.startYear() != null
        && movie.startYear() != null
        && Math.abs(anchor.startYear() - movie.startYear()) <= 10;
  }

  private double genreSimilarity(Set<MovieGenre> left, Set<MovieGenre> right) {
    Set<MovieGenre> leftGenres = genres(left);
    Set<MovieGenre> rightGenres = genres(right);
    if (leftGenres.isEmpty() || rightGenres.isEmpty()) {
      return 0.0;
    }
    Set<MovieGenre> intersection = new HashSet<>(leftGenres);
    intersection.retainAll(rightGenres);
    Set<MovieGenre> union = new HashSet<>(leftGenres);
    union.addAll(rightGenres);
    return (double) intersection.size() / union.size();
  }

  private Set<MovieGenre> genres(Set<MovieGenre> movieGenres) {
    return movieGenres == null ? new HashSet<>() : new HashSet<>(movieGenres);
  }

  private double yearSimilarity(Integer anchorYear, Integer candidateYear) {
    if (anchorYear == null || candidateYear == null) {
      return 0.0;
    }
    return Math.max(0.0, 1.0 - Math.abs(anchorYear - candidateYear) / 40.0);
  }

  private double qualityScore(Float rating, Integer ratingCount) {
    if (rating == null || ratingCount == null || ratingCount <= 0) {
      return 0.0;
    }
    double normalizedRating = Math.clamp(rating / 10.0, 0.0, 1.0);
    double confidence = Math.min(1.0, Math.log1p(ratingCount) / Math.log1p(1_000_000));
    return normalizedRating * confidence;
  }

  private String label(MovieGenre genre) {
    return genre.name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  private record ScoredCandidate(MovieRecommendationCandidate candidate, double baseScore) {}
}
