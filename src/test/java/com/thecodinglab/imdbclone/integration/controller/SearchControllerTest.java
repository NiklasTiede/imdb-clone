package com.thecodinglab.imdbclone.integration.controller;

import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieElasticSearchRepository;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import com.thecodinglab.imdbclone.enums.MovieGenreEnum;
import com.thecodinglab.imdbclone.integration.BaseContainers;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

// spotless:off
@SpringBootTest
@AutoConfigureRestTestClient
@AutoConfigureMockMvc
class SearchControllerTest extends BaseContainers {

  @Autowired private RestTestClient restTestClient;

  @Autowired private MovieRepository movieRepository;

  @Autowired private MovieElasticSearchRepository movieSearchRepository;

  @BeforeEach
  void indexSeedMovies() {
    movieSearchRepository.deleteAll();
    movieSearchRepository.saveAll(movieRepository.findAll());
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
        new MovieSearchRequest(2011, null, null, null, Set.of(MovieGenreEnum.DRAMA), null);

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
}
// spotless:on
