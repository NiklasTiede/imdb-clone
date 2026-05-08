package com.thecodinglab.imdbclone.integration.controller;

import com.thecodinglab.imdbclone.integration.BaseContainers;
import com.thecodinglab.imdbclone.payload.movie.MovieSearchRequest;
import java.util.Collections;
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

  @Test
  void search_success() {
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
}
// spotless:on
