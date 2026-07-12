package com.thecodinglab.imdbclone.catalog.internal.search.evaluation;

import java.util.Comparator;
import java.util.List;

/** Deterministic offline metrics for a versioned set of search judgements and result rankings. */
public class SearchRelevanceEvaluator {

  public SearchRelevanceMetrics evaluate(List<SearchRelevanceCase> cases) {
    if (cases == null || cases.isEmpty()) {
      return new SearchRelevanceMetrics(0, 0, 0, 0, 0, 0, 0);
    }
    int count = cases.size();
    double mrr = cases.stream().mapToDouble(this::reciprocalRank).average().orElse(0);
    double ndcg = cases.stream().mapToDouble(this::ndcgAt10).average().orElse(0);
    double p5 = cases.stream().mapToDouble(case_ -> precisionAt(case_, 5)).average().orElse(0);
    double p10 = cases.stream().mapToDouble(case_ -> precisionAt(case_, 10)).average().orElse(0);
    double zeroRate =
        cases.stream().filter(case_ -> case_.rankedMovieIds().isEmpty()).count() / (double) count;
    int p95Index = Math.max(0, (int) Math.ceil(count * 0.95) - 1);
    double p95Latency =
        cases.stream()
            .map(SearchRelevanceCase::latencyMs)
            .sorted()
            .skip(p95Index)
            .findFirst()
            .orElse(0L);
    return new SearchRelevanceMetrics(count, mrr, ndcg, p5, p10, p95Latency, zeroRate);
  }

  private double reciprocalRank(SearchRelevanceCase case_) {
    for (int index = 0; index < case_.rankedMovieIds().size(); index++) {
      if (grade(case_, case_.rankedMovieIds().get(index)) > 0) return 1.0 / (index + 1);
    }
    return 0;
  }

  private double precisionAt(SearchRelevanceCase case_, int cutoff) {
    return case_.rankedMovieIds().stream()
            .limit(cutoff)
            .filter(movieId -> grade(case_, movieId) > 0)
            .count()
        / (double) cutoff;
  }

  private double ndcgAt10(SearchRelevanceCase case_) {
    double dcg = 0;
    for (int index = 0; index < Math.min(10, case_.rankedMovieIds().size()); index++) {
      dcg += gain(grade(case_, case_.rankedMovieIds().get(index))) / log2(index + 2);
    }
    List<Integer> idealGrades =
        case_.relevanceByMovieId().values().stream()
            .filter(grade -> grade > 0)
            .sorted(Comparator.reverseOrder())
            .limit(10)
            .toList();
    double idealDcg = 0;
    for (int index = 0; index < idealGrades.size(); index++) {
      idealDcg += gain(idealGrades.get(index)) / log2(index + 2);
    }
    return idealDcg == 0 ? 0 : dcg / idealDcg;
  }

  private int grade(SearchRelevanceCase case_, Long movieId) {
    return case_.relevanceByMovieId().getOrDefault(movieId, 0);
  }

  private double gain(int grade) {
    return Math.pow(2, grade) - 1;
  }

  private double log2(int value) {
    return Math.log(value) / Math.log(2);
  }
}
