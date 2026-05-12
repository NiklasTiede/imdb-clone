package com.thecodinglab.imdbclone.account;

import com.thecodinglab.imdbclone.engagement.internal.persistence.Comment;
import com.thecodinglab.imdbclone.engagement.internal.persistence.CommentRepository;
import com.thecodinglab.imdbclone.engagement.internal.persistence.Rating;
import com.thecodinglab.imdbclone.engagement.internal.persistence.RatingRepository;
import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovie;
import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovieRepository;
import com.thecodinglab.imdbclone.support.BaseContainers;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest
@AutoConfigureRestTestClient
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountEngagementControllerTest extends BaseContainers {

  @Autowired private RestTestClient restTestClient;

  @Autowired private CommentRepository commentRepository;

  @Autowired private WatchedMovieRepository watchedMovieRepository;

  @Autowired private RatingRepository ratingRepository;

  @BeforeAll
  void setup() {
    commentRepository.save(new Comment("public profile comment", 2L, 1L));
    watchedMovieRepository.save(WatchedMovie.create(1L, 2L));
    ratingRepository.save(Rating.create(new BigDecimal("8.5"), 1L, 2L));
  }

  @Test
  void getCommentsByAccount_success() {
    restTestClient
        .get()
        .uri("/api/account/test_user_two/comments?page=0&size=10")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.totalElements")
                    .isEqualTo(1)
                    .jsonPath("$.content[0].message")
                    .isEqualTo("public profile comment")
                    .jsonPath("$.content[0].accountId")
                    .isEqualTo(2)
                    .jsonPath("$.content[0].movieId")
                    .isEqualTo(1));
  }

  @Test
  void getWatchlistByAccount_success() {
    restTestClient
        .get()
        .uri("/api/account/test_user_two/watchlist?page=0&size=10")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.totalElements")
                    .isEqualTo(1)
                    .jsonPath("$.content[0].accountId")
                    .isEqualTo(2)
                    .jsonPath("$.content[0].movieId")
                    .isEqualTo(1)
                    .jsonPath("$.content[0].movie.id")
                    .isEqualTo(1));
  }

  @Test
  void getRatingsByAccount_success() {
    restTestClient
        .get()
        .uri("/api/account/test_user_two/ratings?page=0&size=10")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.totalElements")
                    .isEqualTo(1)
                    .jsonPath("$.content[0].rating")
                    .isEqualTo(8.5)
                    .jsonPath("$.content[0].accountId")
                    .isEqualTo(2)
                    .jsonPath("$.content[0].movieId")
                    .isEqualTo(1));
  }

  @Test
  void getAccountEngagement_unknownUsername() {
    restTestClient
        .get()
        .uri("/api/account/missing_user/comments")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isNotFound(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.detail")
                    .isEqualTo("User with username [missing_user] not found in database."));
  }

  @Test
  void getAccountEngagement_rejectsInvalidPagination() {
    restTestClient
        .get()
        .uri("/api/account/test_user_two/comments?page=0&size=31")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isBadRequest(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.detail")
                    .isEqualTo("Page size must not be greater than 30"));
  }
}
