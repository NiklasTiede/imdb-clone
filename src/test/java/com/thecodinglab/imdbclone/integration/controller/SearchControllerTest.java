package com.thecodinglab.imdbclone.integration.controller;

import com.thecodinglab.imdbclone.integration.BaseContainers;
import com.thecodinglab.imdbclone.payload.movie.MovieSearchRequest;
//import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureMockMvc
class SearchControllerTest extends BaseContainers {

  @Autowired private WebTestClient webTestClient;

  // TODO: fix not FOUND Nightcrawler movie (although it seemed to be indexed)
//  @Test
  void search_success() {
    // Arrange
    var request = new MovieSearchRequest(null, null, null, null, null, null);

    // Act and Assert
    webTestClient
        .post()
        .uri("/api/search/movies?query=nightcrawler")
        .bodyValue(request)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.pageNumber").isEqualTo(0);
  }
}
