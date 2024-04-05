package com.thecodinglab.imdbclone.integration.controller;

import com.thecodinglab.imdbclone.integration.BaseContainers;
import com.thecodinglab.imdbclone.payload.movie.MovieSearchRequest;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

// spotless:off
@SpringBootTest
@AutoConfigureMockMvc
class SearchControllerTest extends BaseContainers {

  @Autowired private WebTestClient webTestClient;

  @Test
  void search_success() {
    // Arrange
    var request = new MovieSearchRequest(null, null, null, null, Collections.emptySet(), null);

    // Act and Assert
    webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder
                    .path("/api/search/movies")
                    .queryParam("query", "testMovieOnePri")
                    .build())
            .bodyValue(request)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectAll(spec -> spec.expectStatus().isOk(),
                    spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
                    spec -> spec.expectBody()
                            .jsonPath("$.content[0].primaryTitle").isEqualTo("testMovieOnePri")
            );
  }
}
// spotless:on
