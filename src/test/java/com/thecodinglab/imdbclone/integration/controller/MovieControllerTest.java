package com.thecodinglab.imdbclone.integration.controller;

import com.thecodinglab.imdbclone.integration.BaseContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureMockMvc
class MovieControllerTest extends BaseContainers {

  @Autowired private WebTestClient webTestClient;

  @Test
  void getMovieById_success() {
    // Arrange
    long existingMovie = 2872718L;

    // Act and Assert
    webTestClient
        .get()
        .uri("/api/movie/{movieId}", existingMovie)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.primaryTitle")
        .isEqualTo("Nightcrawler");
  }

  @Test
  void getMovieById_NotFound() {
    // Arrange
    Long nonExistentMovieId = 999999L;

    // Act and Assert
    webTestClient
        .get()
        .uri("/api/movie/{movieId}", nonExistentMovieId)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.detail")
        .isEqualTo("Movie with id [" + nonExistentMovieId + "] not found in database.");
  }

  @Test
  void getMoviesByIds() {}

  @Test
  void createMovie() {}

  @Test
  void updateMovie() {}

  @Test
  void deleteMovie() {}

  @Test
  void searchMoviesByTitle() {}
}
