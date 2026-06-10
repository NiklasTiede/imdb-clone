package com.thecodinglab.imdbclone.catalog;

import static com.thecodinglab.imdbclone.support.SecurityMockUsers.testAdmin;
import static com.thecodinglab.imdbclone.support.SecurityMockUsers.testUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchReindexJobResponse;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchReindexJobStatus;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import com.thecodinglab.imdbclone.catalog.internal.search.MovieSearchService;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentRepository;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchEmbeddingTextBuilder;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchIndexMaintenance;
import com.thecodinglab.imdbclone.support.BaseContainers;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.data.core.OpenSearchOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;

// spotless:off
@SpringBootTest
@AutoConfigureRestTestClient
@AutoConfigureMockMvc
class SearchControllerTest extends BaseContainers {

  @Autowired private RestTestClient restTestClient;

  @Autowired private MockMvc mockMvc;

  @Autowired private MovieRepository movieRepository;

  @Autowired private MovieSearchDocumentRepository movieSearchRepository;

  @Autowired private MovieSearchIndexMaintenance movieSearchIndexMaintenance;

  @Autowired private MovieSearchService movieSearchService;

  @Autowired private MovieSearchEmbeddingTextBuilder movieSearchEmbeddingTextBuilder;

  @Autowired private OpenSearchOperations openSearchOperations;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @BeforeEach
  void indexSeedMovies() {
    movieSearchIndexMaintenance.reindexMovies();
  }

  @Test
  void search_withTitleQuery_returnsMatchingMovie() {
    // Arrange
    var request = new MovieSearchRequest(null, null, null, null, Collections.emptySet(), null);

    // Act and Assert
    restTestClient
            .post()
            .uri(uriBuilder -> uriBuilder
                    .path("/api/search/movies")
                    .queryParam("query", "testMovieOnePri")
                    .build())
            .body(request)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectAll(spec -> spec.expectStatus().isOk(),
                    spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
                    spec -> spec.expectBody()
                            .jsonPath("$.page").isEqualTo(0)
                            .jsonPath("$.number").doesNotExist()
                            .jsonPath("$.pageable").doesNotExist()
                            .jsonPath("$.content[0].id").isEqualTo(1)
                            .jsonPath("$.content[0].primaryTitle").isEqualTo("testMovieOnePri")
            );
  }

  @Test
  void search_withBlankQueryAndFilters_returnsMatchingMovies() {
    // Arrange
    var request =
        new MovieSearchRequest(2011, null, null, null, Set.of(MovieGenre.DRAMA), null);

    // Act and Assert
    restTestClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/api/search/movies")
                    .queryParam("query", "")
                    .queryParam("page", 0)
                    .queryParam("size", 20)
                    .build())
        .body(request)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.totalElements")
                    .isEqualTo(1)
                    .jsonPath("$.content[0].id")
                    .isEqualTo(2)
                    .jsonPath("$.content[0].primaryTitle")
                    .isEqualTo("testMovieTwoPri"));
  }

  @Test
  void reindexMovies_rebuildsOpenSearchFromPostgresql() throws Exception {
    movieSearchRepository.deleteAll();
    Assertions.assertThat(movieSearchRepository.count()).isZero();
    long expectedMovies = movieRepository.count();

    MovieSearchReindexJobResponse startedJob =
        objectMapper.readValue(
            mockMvc
                .perform(
                    post("/api/search/movies/reindex")
                        .with(testAdmin())
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.status").value(MovieSearchReindexJobStatus.RUNNING.name()))
                .andExpect(jsonPath("$.indexedMovies").value(0))
                .andExpect(jsonPath("$.totalMovies").value((int) expectedMovies))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            MovieSearchReindexJobResponse.class);

    MovieSearchReindexJobResponse completedJob = awaitCompletedReindex(startedJob.jobId());
    Assertions.assertThat(completedJob.status()).isEqualTo(MovieSearchReindexJobStatus.COMPLETED);
    Assertions.assertThat(completedJob.indexedMovies()).isEqualTo(expectedMovies);

    Assertions.assertThat(movieSearchRepository.count()).isEqualTo(expectedMovies);
    Map<String, Object> embeddingMapping =
        propertyMapping(openSearchOperations.indexOps(MovieSearchDocument.class).getMapping(), "embedding");
    Map<String, Object> primaryTitleMapping =
        propertyMapping(openSearchOperations.indexOps(MovieSearchDocument.class).getMapping(), "primaryTitle");
    Map<String, Object> originalTitleMapping =
        propertyMapping(openSearchOperations.indexOps(MovieSearchDocument.class).getMapping(), "originalTitle");

    Assertions.assertThat(primaryTitleMapping).containsEntry("type", "search_as_you_type");
    Assertions.assertThat(originalTitleMapping).containsEntry("type", "search_as_you_type");
    Assertions.assertThat(embeddingMapping).containsEntry("type", "knn_vector");
    Assertions.assertThat(embeddingMapping).containsEntry("dimension", 768);

    MovieSearchDocument indexedMovie = movieSearchRepository.findById(1L).orElseThrow();
    Assertions.assertThat(indexedMovie.getEmbeddingModel()).isEqualTo("embeddinggemma");
    Assertions.assertThat(indexedMovie.getEmbeddingTextVersion()).isEqualTo("movie-search-v1");
  }

  @Test
  void semanticSearch_returnsNearestEmbeddedMovie() {
    String query =
        movieSearchEmbeddingTextBuilder.build(movieRepository.findById(1L).orElseThrow());
    var request = new MovieSearchRequest(null, null, null, null, Collections.emptySet(), null);

    var response = movieSearchService.searchMoviesSemantically(query, request, 0, 20);

    Assertions.assertThat(response.getContent())
        .isNotEmpty()
        .first()
        .extracting(MovieRecord::id)
        .isEqualTo(1L);
  }

  @Test
  void reindexMovies_withUserRoleIsForbidden() throws Exception {
    mockMvc
        .perform(
            post("/api/search/movies/reindex")
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> propertyMapping(Map<String, Object> mapping, String propertyName) {
    Map<String, Object> properties = (Map<String, Object>) mapping.get("properties");
    return (Map<String, Object>) properties.get(propertyName);
  }

  private MovieSearchReindexJobResponse awaitCompletedReindex(UUID jobId) throws Exception {
    MovieSearchReindexJobResponse latestJob = null;
    for (int attempt = 0; attempt < 50; attempt++) {
      latestJob =
          objectMapper.readValue(
              mockMvc
                  .perform(
                      get("/api/search/movies/reindex/{jobId}", jobId)
                          .with(testAdmin())
                          .accept(MediaType.APPLICATION_JSON))
                  .andExpect(status().isOk())
                  .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                  .andReturn()
                  .getResponse()
                  .getContentAsString(),
              MovieSearchReindexJobResponse.class);
      if (latestJob.status() != MovieSearchReindexJobStatus.RUNNING) {
        return latestJob;
      }
      sleepBeforeNextStatusCheck();
    }
    throw new AssertionError("Timed out waiting for reindex job [%s], latest status [%s]".formatted(jobId, latestJob));
  }

  private static void sleepBeforeNextStatusCheck() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for reindex completion", ex);
    }
  }
}
// spotless:on
