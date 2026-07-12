package com.thecodinglab.imdbclone.catalog.internal.search.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("search-evaluation")
class SearchRelevanceEvaluationTest {

  @Test
  void reportsTheVersionedBaselineMetrics() throws Exception {
    List<SearchRelevanceCase> cases = loadCases();
    SearchRelevanceMetrics metrics = new SearchRelevanceEvaluator().evaluate(cases);

    assertThat(metrics.evaluatedQueries()).isEqualTo(3);
    assertThat(metrics.meanReciprocalRank())
        .isCloseTo(2.0 / 3.0, org.assertj.core.data.Offset.offset(0.0001));
    assertThat(metrics.precisionAt5()).isCloseTo(0.4, org.assertj.core.data.Offset.offset(0.0001));
    assertThat(metrics.precisionAt10()).isCloseTo(0.2, org.assertj.core.data.Offset.offset(0.0001));
    assertThat(metrics.zeroResultRate())
        .isCloseTo(1.0 / 3.0, org.assertj.core.data.Offset.offset(0.0001));
    assertThat(metrics.p95LatencyMs()).isEqualTo(58);
    System.out.printf(
        "search-relevance-v1: MRR=%.3f nDCG@10=%.3f P@5=%.3f P@10=%.3f zero-result=%.3f p95-latency=%.0fms%n",
        metrics.meanReciprocalRank(),
        metrics.ndcgAt10(),
        metrics.precisionAt5(),
        metrics.precisionAt10(),
        metrics.zeroResultRate(),
        metrics.p95LatencyMs());
  }

  private List<SearchRelevanceCase> loadCases() throws Exception {
    try (InputStream stream = getClass().getResourceAsStream("/search/relevance-v1.json")) {
      JsonNode root = new ObjectMapper().readTree(stream);
      List<SearchRelevanceCase> cases = new ArrayList<>();
      for (JsonNode node : root.path("cases")) {
        Map<Long, Integer> grades = new HashMap<>();
        node.path("relevanceByMovieId")
            .properties()
            .forEach(entry -> grades.put(Long.valueOf(entry.getKey()), entry.getValue().asInt()));
        List<Long> ranked = new ArrayList<>();
        node.path("rankedMovieIds").forEach(id -> ranked.add(id.asLong()));
        cases.add(
            new SearchRelevanceCase(
                node.path("id").asText(),
                node.path("query").asText(),
                grades,
                ranked,
                node.path("latencyMs").asLong()));
      }
      return cases;
    }
  }
}
