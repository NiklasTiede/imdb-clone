package com.thecodinglab.imdbclone.recommendation;

import static com.thecodinglab.imdbclone.support.SecurityMockUsers.testAdmin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchIndexMaintenance;
import com.thecodinglab.imdbclone.support.BaseControllerIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;

class RecommendationControllerTest extends BaseControllerIntegrationTest {

  @Autowired private RestTestClient restTestClient;
  @Autowired private MovieSearchIndexMaintenance movieSearchIndexMaintenance;
  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void indexSeedMovies() {
    movieSearchIndexMaintenance.reindexMovies();
  }

  @Test
  void similarMovies_isPublicAndExcludesAnchor() {
    restTestClient
        .get()
        .uri("/api/recommendations/movies/{movieId}/similar?limit=3", 1)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.strategy")
                    .isEqualTo("content-v1")
                    .jsonPath("$.items")
                    .isArray()
                    .jsonPath("$.items[?(@.movie.id == 1)]")
                    .isEmpty());
  }

  @Test
  void similarMovies_rejectsInvalidLimit() {
    restTestClient
        .get()
        .uri("/api/recommendations/movies/{movieId}/similar?limit=31", 1)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void similarMovies_returnsNotFoundForUnknownMovie() {
    restTestClient
        .get()
        .uri("/api/recommendations/movies/{movieId}/similar", 999_999)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void homeFeed_isPublicAndReturnsASeededResponse() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendations/home-feed")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\"feedInstanceId\":\"controller-test-feed\",\"excludedMovieIds\":[]}"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.strategyVersion").value("home-structured-v1"))
        .andExpect(jsonPath("$.seed").isNotEmpty())
        .andExpect(jsonPath("$.sections").isArray());
  }

  @Test
  void discoveryEvents_arePublicIdempotentAndDoNotPersistRawSessionIds() throws Exception {
    String request =
        """
        {
          "eventId":"event-telemetry-0001",
          "eventType":"MOVIE_OPEN",
          "sessionId":"anonymous-browser-session-0001",
          "feedInstanceId":"home-feed-instance-0001",
          "sectionId":"new-and-noteworthy",
          "position":2,
          "movieId":1,
          "strategyVersion":"home-structured-v1"
        }
        """;

    for (int attempt = 0; attempt < 2; attempt++) {
      mockMvc
          .perform(
              post("/api/recommendations/discovery-events")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(request))
          .andExpect(status().isNoContent());
    }

    Integer persisted =
        jdbcTemplate.queryForObject(
            "select count(*) from discovery_event where event_id = ?",
            Integer.class,
            "event-telemetry-0001");
    String sessionHash =
        jdbcTemplate.queryForObject(
            "select session_hash from discovery_event where event_id = ?",
            String.class,
            "event-telemetry-0001");
    org.assertj.core.api.Assertions.assertThat(persisted).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(sessionHash)
        .isNotEqualTo("anonymous-browser-session-0001");
  }

  @Test
  void discoveryEventSummary_requiresAdminAndReportsRecordedEvents() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendations/discovery-events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"eventId":"event-summary-0001","eventType":"SECTION_IMPRESSION","sessionId":"anonymous-browser-session-0002","feedInstanceId":"home-feed-instance-0002","sectionId":"quiet-grief","position":0,"strategyVersion":"home-structured-v1"}
                    """))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/recommendations/discovery-events/summary"))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/recommendations/discovery-events/summary")
                .with(testAdmin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventsByType.SECTION_IMPRESSION").value(1));
  }
}
