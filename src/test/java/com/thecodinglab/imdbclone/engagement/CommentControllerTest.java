package com.thecodinglab.imdbclone.engagement;

import static com.thecodinglab.imdbclone.support.SecurityMockUsers.testUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thecodinglab.imdbclone.engagement.api.CommentRecord;
import com.thecodinglab.imdbclone.engagement.api.CreateCommentRequest;
import com.thecodinglab.imdbclone.engagement.api.UpdateCommentRequest;
import com.thecodinglab.imdbclone.engagement.internal.persistence.CommentRepository;
import com.thecodinglab.imdbclone.support.BaseControllerIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;

// spotless:off
class CommentControllerTest extends BaseControllerIntegrationTest {

  private static final long MOVIE_ID = 1L;
  private static final long ACCOUNT_ID = 2L;
  private static final String TEST_COMMENT_PREFIX = "schema-regression-comment";

  @Autowired private RestTestClient restTestClient;

  @Autowired private MockMvc mockMvc;

  @Autowired private CommentRepository commentRepository;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @AfterEach
  void cleanup() {
    commentRepository.findAll().stream()
        .filter(comment -> comment.getMessage() != null)
        .filter(comment -> comment.getMessage().startsWith(TEST_COMMENT_PREFIX))
        .forEach(commentRepository::delete);
  }

  @Test
  void createGetUpdateAndDeleteComment_success() throws Exception {
    var createRequest = new CreateCommentRequest(TEST_COMMENT_PREFIX + " created");

    var createdComment =
        objectMapper.readValue(
            mockMvc
                .perform(
                    post("/api/comment/{movieId}", MOVIE_ID)
                        .with(testUser())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            CommentRecord.class);

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
                    .jsonPath("$.movieId").isEqualTo(MOVIE_ID)
                    .jsonPath("$.createdAtInUtc").isNotEmpty()
                    .jsonPath("$.modifiedAtInUtc").isNotEmpty());

    var updateRequest = new UpdateCommentRequest(TEST_COMMENT_PREFIX + " updated");

    mockMvc
        .perform(
            put("/api/comment/{commentId}", createdComment.id())
                .with(testUser())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(createdComment.id()))
        .andExpect(jsonPath("$.message").value(TEST_COMMENT_PREFIX + " updated"))
        .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID))
        .andExpect(jsonPath("$.movieId").value(MOVIE_ID));

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

    mockMvc
        .perform(
            delete("/api/comment/{commentId}", createdComment.id())
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

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
  void createComment_unauthenticated() throws Exception {
    var request = new CreateCommentRequest(TEST_COMMENT_PREFIX + " unauthorized");

    mockMvc
        .perform(
            post("/api/comment/{movieId}", MOVIE_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void createComment_rejectsBlankMessage() throws Exception {
    var request = new CreateCommentRequest("   ");

    mockMvc
        .perform(
            post("/api/comment/{movieId}", MOVIE_ID)
                .with(testUser())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.message").value("message must not be blank"));
  }
}
// spotless:on
