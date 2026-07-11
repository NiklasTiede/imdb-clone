package com.thecodinglab.imdbclone.engagement;

import static com.thecodinglab.imdbclone.support.SecurityMockUsers.testUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import com.thecodinglab.imdbclone.engagement.internal.persistence.RatingRepository;
import com.thecodinglab.imdbclone.support.BaseContainers;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class RatingControllerTest extends BaseContainers {

  private static final long MOVIE_ID = 1L;
  private static final long ACCOUNT_ID = 2L;

  @Autowired private RestTestClient restTestClient;

  @Autowired private MockMvc mockMvc;

  @Autowired private RatingRepository ratingRepository;

  @Autowired private MovieRepository movieRepository;

  @BeforeEach
  @AfterEach
  void cleanup() {
    ratingRepository
        .findByIdAccountIdAndIdMovieId(ACCOUNT_ID, MOVIE_ID)
        .ifPresent(ratingRepository::delete);
    var movie = movieRepository.getMovieById(MOVIE_ID);
    movie.setRating(null);
    movie.setRatingCount(0);
    movie.setRatingSum(BigDecimal.ZERO);
    movieRepository.save(movie);
  }

  @Test
  void rateListAndDeleteMovie_success() throws Exception {
    mockMvc
        .perform(
            put("/api/movie-rating/{movieId}/rating-score/{score}", MOVIE_ID, "8.5")
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.rating").value(8.5))
        .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID))
        .andExpect(jsonPath("$.movieId").value(MOVIE_ID));

    restTestClient
        .get()
        .uri("/api/account/{username}/ratings", "test_user_two")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.page").isEqualTo(0)
                    .jsonPath("$.number").doesNotExist()
                    .jsonPath("$.pageable").doesNotExist()
                    .jsonPath("$.content[0].rating").isEqualTo(8.5)
                    .jsonPath("$.content[0].accountId").isEqualTo(ACCOUNT_ID)
                    .jsonPath("$.content[0].movieId").isEqualTo(MOVIE_ID));

    mockMvc
        .perform(
            delete("/api/movie-rating/{movieId}", MOVIE_ID)
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            delete("/api/movie-rating/{movieId}", MOVIE_ID)
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void rateMovie_acceptsTenPointZeroScore() throws Exception {
    mockMvc
        .perform(
            put("/api/movie-rating/{movieId}/rating-score/{score}", MOVIE_ID, "10.0")
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.rating").value(10.0));
  }

  @Test
  void rateMovie_rejectsTenPointOneScore() throws Exception {
    mockMvc
        .perform(
            put("/api/movie-rating/{movieId}/rating-score/{score}", MOVIE_ID, "10.1")
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Score must be between 0 and 10"));
  }

  @Test
  void rateMovie_rejectsScoreSlightlyAboveTen() throws Exception {
    mockMvc
        .perform(
            put("/api/movie-rating/{movieId}/rating-score/{score}", MOVIE_ID, "10.05")
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Score must be between 0 and 10"));
  }

  @Test
  void rateMovie_rejectsScoreThatWouldBeRoundedByTheDatabase() throws Exception {
    mockMvc
        .perform(
            put("/api/movie-rating/{movieId}/rating-score/{score}", MOVIE_ID, "8.55")
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Score must use at most one decimal place"));
  }

  @Test
  void rateMovie_rejectsNegativeScore() throws Exception {
    mockMvc
        .perform(
            put("/api/movie-rating/{movieId}/rating-score/{score}", MOVIE_ID, "-0.1")
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Score must be between 0 and 10"));
  }

  @Test
  void rateUpdateAndDeleteMovie_updatesMovieRatingAggregateImmediately() throws Exception {
    mockMvc
        .perform(
            put("/api/movie-rating/{movieId}/rating-score/{score}", MOVIE_ID, "8.5")
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());

    var movieAfterCreate = movieRepository.getMovieById(MOVIE_ID);
    assertThat(movieAfterCreate.getRating()).isEqualByComparingTo(new BigDecimal("8.5"));
    assertThat(movieAfterCreate.getRatingCount()).isEqualTo(1);

    mockMvc
        .perform(
            put("/api/movie-rating/{movieId}/rating-score/{score}", MOVIE_ID, "6.5")
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());

    var movieAfterUpdate = movieRepository.getMovieById(MOVIE_ID);
    assertThat(movieAfterUpdate.getRating()).isEqualByComparingTo(new BigDecimal("6.5"));
    assertThat(movieAfterUpdate.getRatingCount()).isEqualTo(1);

    mockMvc
        .perform(
            delete("/api/movie-rating/{movieId}", MOVIE_ID)
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    var movieAfterDelete = movieRepository.getMovieById(MOVIE_ID);
    assertThat(movieAfterDelete.getRating()).isNull();
    assertThat(movieAfterDelete.getRatingCount()).isZero();
  }

  @Test
  void rateMovie_unauthenticated() throws Exception {
    mockMvc
        .perform(
            put("/api/movie-rating/{movieId}/rating-score/{score}", MOVIE_ID, "8.5")
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
// spotless:on
