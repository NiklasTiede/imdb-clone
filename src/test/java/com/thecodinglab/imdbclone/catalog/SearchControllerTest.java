package com.thecodinglab.imdbclone.catalog;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import com.thecodinglab.imdbclone.catalog.internal.search.MovieSearchService;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentRepository;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchEmbeddingTextBuilder;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchIndexMaintenance;
import com.thecodinglab.imdbclone.identity.api.AuthenticationService;
import com.thecodinglab.imdbclone.identity.api.LoginRequest;
import com.thecodinglab.imdbclone.support.BaseContainers;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.opensearch.data.core.OpenSearchOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.client.RestTestClient;

// spotless:off
@SpringBootTest
@AutoConfigureRestTestClient
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchControllerTest extends BaseContainers {

  @Autowired private RestTestClient restTestClient;

  @Autowired private AuthenticationService authenticationService;

  @Autowired private MovieRepository movieRepository;

  @Autowired private MovieSearchDocumentRepository movieSearchRepository;

  @Autowired private MovieSearchIndexMaintenance movieSearchIndexMaintenance;

  @Autowired private MovieSearchService movieSearchService;

  @Autowired private MovieSearchEmbeddingTextBuilder movieSearchEmbeddingTextBuilder;

  @Autowired private OpenSearchOperations openSearchOperations;

  private String adminToken;
  private String userToken;

  @BeforeAll
  void setup() {
    var adminRequest = new LoginRequest("test_user_one", "Encrypted!Pa55worD");
    var adminLogin = authenticationService.loginUser(adminRequest);
    adminToken = "%s %s".formatted(adminLogin.getTokenType(), adminLogin.getAccessToken());
    var userRequest = new LoginRequest("test_user_two", "Encrypted!Pa55worD");
    var userLogin = authenticationService.loginUser(userRequest);
    userToken = "%s %s".formatted(userLogin.getTokenType(), userLogin.getAccessToken());
    SecurityContextHolder.clearContext();
  }

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
  void reindexMovies_rebuildsElasticsearchFromPostgresql() {
    movieSearchRepository.deleteAll();
    Assertions.assertThat(movieSearchRepository.count()).isZero();
    long expectedMovies = movieRepository.count();

    restTestClient
        .post()
        .uri("/api/search/movies/reindex")
        .header("Authorization", adminToken)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.indexedMovies")
                    .isEqualTo((int) expectedMovies));

    Assertions.assertThat(movieSearchRepository.count()).isEqualTo(expectedMovies);
    Map<String, Object> embeddingMapping =
        propertyMapping(openSearchOperations.indexOps(MovieSearchDocument.class).getMapping(), "embedding");
    Map<String, Object> primaryTitleMapping =
        propertyMapping(openSearchOperations.indexOps(MovieSearchDocument.class).getMapping(), "primaryTitle");
    Map<String, Object> originalTitleMapping =
        propertyMapping(openSearchOperations.indexOps(MovieSearchDocument.class).getMapping(), "originalTitle");

    Assertions.assertThat(primaryTitleMapping).containsEntry("type", "search_as_you_type");
    Assertions.assertThat(originalTitleMapping).containsEntry("type", "search_as_you_type");
    Assertions.assertThat(embeddingMapping).containsEntry("type", "dense_vector");
    Assertions.assertThat(embeddingMapping).containsEntry("dims", 768);
    Assertions.assertThat(embeddingMapping).containsEntry("index", true);
    Assertions.assertThat(embeddingMapping).containsEntry("similarity", "cosine");

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
  void reindexMovies_withUserRoleIsForbidden() {
    restTestClient
        .post()
        .uri("/api/search/movies/reindex")
        .header("Authorization", userToken)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> propertyMapping(Map<String, Object> mapping, String propertyName) {
    Map<String, Object> properties = (Map<String, Object>) mapping.get("properties");
    return (Map<String, Object>) properties.get(propertyName);
  }
}
// spotless:on
