package com.thecodinglab.imdbclone.catalog.internal.search.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Tag("live-search-evaluation")
@EnabledIfEnvironmentVariable(named = "IMDB_CLONE_LIVE_SEARCH_EVALUATION", matches = "true")
class LiveSearchRelevanceEvaluationTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final int RESULT_SIZE = 10;

  @Test
  void liveSearchMeetsTheVersionedRelevanceAndLatencyBaseline() throws Exception {
    JsonNode fixture = loadFixture();
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    List<SearchRelevanceCase> cases = new ArrayList<>();

    for (JsonNode judgement : fixture.path("cases")) {
      cases.add(runCase(httpClient, judgement));
    }

    SearchRelevanceMetrics metrics = new SearchRelevanceEvaluator().evaluate(cases);
    System.out.printf(
        "%s corpus=%s model=%s queries=%d MRR=%.3f nDCG@10=%.3f P@5=%.3f "
            + "P@10=%.3f zero-result=%.3f p95-latency=%.0fms%n",
        fixture.path("version").asText(),
        fixture.path("corpus").asText(),
        fixture.path("embeddingModel").asText(),
        metrics.evaluatedQueries(),
        metrics.meanReciprocalRank(),
        metrics.ndcgAt10(),
        metrics.precisionAt5(),
        metrics.precisionAt10(),
        metrics.zeroResultRate(),
        metrics.p95LatencyMs());

    assertThat(metrics.evaluatedQueries()).isEqualTo(fixture.path("cases").size());
    assertThat(metrics.meanReciprocalRank()).isGreaterThanOrEqualTo(0.85);
    assertThat(metrics.ndcgAt10()).isGreaterThanOrEqualTo(0.75);
    assertThat(metrics.precisionAt5()).isGreaterThanOrEqualTo(0.18);
    assertThat(metrics.zeroResultRate()).isZero();
    assertThat(metrics.p95LatencyMs()).isLessThan(1_500);
  }

  private SearchRelevanceCase runCase(HttpClient httpClient, JsonNode judgement) throws Exception {
    String query = judgement.path("query").asText();
    String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
    String baseUrl =
        System.getenv().getOrDefault("IMDB_CLONE_SEARCH_BASE_URL", "http://localhost:8080");
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "%s/api/search/movies?query=%s&page=0&size=%d"
                        .formatted(baseUrl, encodedQuery, RESULT_SIZE)))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .build();

    long startedAt = System.nanoTime();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    assertThat(response.statusCode()).isBetween(200, 299);

    JsonNode body = OBJECT_MAPPER.readTree(response.body());
    List<Long> rankedMovieIds = new ArrayList<>();
    body.path("content").forEach(movie -> rankedMovieIds.add(movie.path("id").asLong()));

    Map<Long, Integer> relevanceByMovieId = new HashMap<>();
    judgement
        .path("relevanceByMovieId")
        .properties()
        .forEach(
            entry ->
                relevanceByMovieId.put(Long.valueOf(entry.getKey()), entry.getValue().asInt()));
    return new SearchRelevanceCase(
        judgement.path("id").asText(), query, relevanceByMovieId, rankedMovieIds, latencyMs);
  }

  private JsonNode loadFixture() throws Exception {
    try (InputStream stream = getClass().getResourceAsStream("/search/relevance-live-v1.json")) {
      return OBJECT_MAPPER.readTree(stream);
    }
  }
}
