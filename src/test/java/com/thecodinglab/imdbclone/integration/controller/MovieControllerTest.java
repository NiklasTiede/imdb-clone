package com.thecodinglab.imdbclone.integration.controller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.thecodinglab.imdbclone.integration.BaseContainers;
import com.thecodinglab.imdbclone.payload.authentication.LoginRequest;
import com.thecodinglab.imdbclone.payload.movie.MovieIdsRequest;
import com.thecodinglab.imdbclone.payload.movie.MovieRequest;
import com.thecodinglab.imdbclone.repository.MovieElasticSearchRepository;
import com.thecodinglab.imdbclone.repository.MovieRepository;
import com.thecodinglab.imdbclone.service.AuthenticationService;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

// spotless:off
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MovieControllerTest extends BaseContainers {

  @Autowired private WebTestClient webTestClient;

  @Autowired private AuthenticationService authenticationService;

  @Autowired private MovieRepository movieRepository;

  @Autowired private MovieElasticSearchRepository movieElasticSearchRepository;

  private String adminToken;

  @BeforeAll
  void setup() {
    var adminRequest = new LoginRequest("test_user_one", "Encrypted!Pa55worD");
    var adminLogin = authenticationService.loginUser(adminRequest);
    adminToken = "%s %s".formatted(adminLogin.getTokenType(), adminLogin.getAccessToken());
  }

  @Test
  void getMovieById_badRequest() {
    // Arrange
    var invalidIdFormat = "abc";

    // Act and Assert
    webTestClient
            .get()
            .uri("/api/movie/{movieId}", invalidIdFormat)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectAll(
                    spec -> spec.expectStatus().isBadRequest(),
                    spec -> spec.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON),
                    spec -> spec.expectBody()
                            .jsonPath("$.detail").isEqualTo("Failed to convert 'movieId' with value: '%s'".formatted(invalidIdFormat))
            );
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
            .expectAll(
                    spec -> spec.expectStatus().isNotFound(),
                    spec -> spec.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON),
                    spec -> spec.expectBody()
                            .jsonPath("$.detail").isEqualTo("Movie with id [%d] not found in database.".formatted(nonExistentMovieId))
            );
  }

  @Test
  void getMovieById_success() {
    // Arrange
    long existingMovie = 1L;

    // Act and Assert
    webTestClient
            .get()
            .uri("/api/movie/{movieId}", existingMovie)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectAll(
                    spec -> spec.expectStatus().isOk(),
                    spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
                    spec -> spec.expectBody()
                            .jsonPath("$.primaryTitle").isEqualTo("testMovieOnePri")
                            .jsonPath("$.startYear").isEqualTo(2010)
            );
  }

  @Test
  void getMoviesByIds_emptyResponse() {
    // Arrange
    var nonExistingMovies = new MovieIdsRequest(List.of(999999L));

    // Act and Assert
    webTestClient
            .post()
            .uri("/api/movie/get-movies")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(nonExistingMovies)
            .exchange()
            .expectAll(
                    spec -> spec.expectStatus().isOk(),
                    spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
                    spec -> spec.expectBody()
                            .jsonPath("$.content").exists()
                            .jsonPath("$.content[0].primaryTitle").doesNotExist()
            );
  }

  @Test
  void getMoviesByIds_success() {
    // Arrange
    var existingMovies = new MovieIdsRequest(List.of(1L, 2L));

    // Act and Assert
    webTestClient
            .post()
            .uri("/api/movie/get-movies")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(existingMovies)
            .exchange()
            .expectAll(
                    spec -> spec.expectStatus().isOk(),
                    spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
                    spec -> spec.expectBody()
                            .jsonPath("$.content[0].primaryTitle").isEqualTo("testMovieOnePri")
                            .jsonPath("$.content[1].primaryTitle").isEqualTo("testMovieTwoPri")
            );
  }

  @Test
  void createMovie_success() {
    // Arrange
    var movieRequest = new MovieRequest(
            "test movie",
            "test movie",
            2015,
            2015,
            105,
            null,
            null,
            false
    );

    // Act and Assert
    webTestClient
            .post()
            .uri("/api/movie/create-movie")
            .header("Authorization", adminToken)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(movieRequest)
            .exchange()
            .expectAll(
                    spec -> spec.expectStatus().isCreated(),
                    spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
                    spec -> spec.expectBody()
                            .jsonPath("$.primaryTitle").isEqualTo("test movie")
                            .jsonPath("$.startYear").isEqualTo(2015)
            );

    var movieEntity = movieRepository.getMovieById(3L);
    assertThat(movieEntity.getPrimaryTitle()).isEqualTo("test movie");
    assertThat(movieEntity.getOriginalTitle()).isNotEqualTo("no movie");

    var movieDocument = movieElasticSearchRepository.findById(3L);
    assertThat(movieDocument).isPresent();
    movieDocument.ifPresent(r -> assertThat(r.getPrimaryTitle()).isEqualTo("test movie"));
    movieDocument.ifPresent(r -> assertThat(r.getPrimaryTitle()).isNotEqualTo("no movie"));
  }

  @Test
  void updateMovie() {}

  @Test
  void deleteMovie() {}
}
// spotless:on
