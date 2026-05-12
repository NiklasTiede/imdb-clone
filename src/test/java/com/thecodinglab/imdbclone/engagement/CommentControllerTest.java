package com.thecodinglab.imdbclone.engagement;

import com.thecodinglab.imdbclone.engagement.api.CommentRecord;
import com.thecodinglab.imdbclone.engagement.api.CreateCommentRequest;
import com.thecodinglab.imdbclone.engagement.api.UpdateCommentRequest;
import com.thecodinglab.imdbclone.engagement.internal.persistence.CommentRepository;
import com.thecodinglab.imdbclone.identity.api.AuthenticationService;
import com.thecodinglab.imdbclone.identity.api.LoginRequest;
import com.thecodinglab.imdbclone.support.BaseContainers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
class CommentControllerTest extends BaseContainers {

  private static final long MOVIE_ID = 1L;
  private static final long ACCOUNT_ID = 2L;
  private static final String TEST_COMMENT_PREFIX = "schema-regression-comment";

  @Autowired private RestTestClient restTestClient;

  @Autowired private AuthenticationService authenticationService;

  @Autowired private CommentRepository commentRepository;

  private String userToken;

  @BeforeAll
  void setup() {
    var userRequest = new LoginRequest("test_user_two", "Encrypted!Pa55worD");
    var userLogin = authenticationService.loginUser(userRequest);
    userToken = "%s %s".formatted(userLogin.getTokenType(), userLogin.getAccessToken());
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void cleanup() {
    commentRepository.findAll().stream()
        .filter(comment -> comment.getMessage() != null)
        .filter(comment -> comment.getMessage().startsWith(TEST_COMMENT_PREFIX))
        .forEach(commentRepository::delete);
  }

  @Test
  void createGetUpdateAndDeleteComment_success() {
    var createRequest = new CreateCommentRequest(TEST_COMMENT_PREFIX + " created");

    var createdComment =
        restTestClient
            .post()
            .uri("/api/comment/{movieId}", MOVIE_ID)
            .header("Authorization", userToken)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(createRequest)
            .exchange()
            .expectAll(
                spec -> spec.expectStatus().isCreated(),
                spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON))
            .expectBody(CommentRecord.class)
            .returnResult()
            .getResponseBody();

    restTestClient
        .get()
        .uri("/api/comment/{commentId}", createdComment.id())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.id").isEqualTo(createdComment.id())
                    .jsonPath("$.message").isEqualTo(TEST_COMMENT_PREFIX + " created")
                    .jsonPath("$.accountId").isEqualTo(ACCOUNT_ID)
                    .jsonPath("$.movieId").isEqualTo(MOVIE_ID));

    var updateRequest = new UpdateCommentRequest(TEST_COMMENT_PREFIX + " updated");

    restTestClient
        .put()
        .uri("/api/comment/{commentId}", createdComment.id())
        .header("Authorization", userToken)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(updateRequest)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.id").isEqualTo(createdComment.id())
                    .jsonPath("$.message").isEqualTo(TEST_COMMENT_PREFIX + " updated")
                    .jsonPath("$.accountId").isEqualTo(ACCOUNT_ID)
                    .jsonPath("$.movieId").isEqualTo(MOVIE_ID));

    restTestClient
        .get()
        .uri("/api/comment/{movieId}/comments", MOVIE_ID)
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
                    .jsonPath("$.content[0].message").isEqualTo(TEST_COMMENT_PREFIX + " updated")
                    .jsonPath("$.content[0].accountId").isEqualTo(ACCOUNT_ID)
                    .jsonPath("$.content[0].movieId").isEqualTo(MOVIE_ID));

    restTestClient
        .get()
        .uri("/api/account/{username}/comments", "test_user_two")
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
                    .jsonPath("$.content[0].message").isEqualTo(TEST_COMMENT_PREFIX + " updated")
                    .jsonPath("$.content[0].accountId").isEqualTo(ACCOUNT_ID)
                    .jsonPath("$.content[0].movieId").isEqualTo(MOVIE_ID));

    restTestClient
        .delete()
        .uri("/api/comment/{commentId}", createdComment.id())
        .header("Authorization", userToken)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNoContent();

    restTestClient
        .get()
        .uri("/api/comment/{commentId}", createdComment.id())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isNotFound(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void createComment_unauthenticated() {
    var request = new CreateCommentRequest(TEST_COMMENT_PREFIX + " unauthorized");

    restTestClient
        .post()
        .uri("/api/comment/{movieId}", MOVIE_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(request)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isUnauthorized(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
// spotless:on
