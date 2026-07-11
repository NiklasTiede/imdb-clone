package com.thecodinglab.imdbclone.catalog;

import static com.thecodinglab.imdbclone.support.SecurityMockUsers.testAdmin;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieIdsRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentRepository;
import com.thecodinglab.imdbclone.catalog.internal.search.projection.MovieSearchProjectionTaskHandler;
import com.thecodinglab.imdbclone.support.BaseControllerIntegrationTest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;

// spotless:off
class MovieControllerTest extends BaseControllerIntegrationTest {

  private static final String TEST_MOVIE_PREFIX = "movie-controller-integration-test";

  @Autowired private RestTestClient restTestClient;

  @Autowired private MockMvc mockMvc;

  @Autowired private MovieRepository movieRepository;

  @Autowired private MovieSearchDocumentRepository movieSearchDocumentRepository;

  @Autowired private MovieSearchProjectionTaskHandler movieSearchProjectionTaskHandler;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @AfterEach
  void cleanup() {
    jdbcTemplate.update("delete from scheduled_tasks where task_name = ?", "movie-search-projection");
    movieRepository.findAll().stream()
        .filter(movie -> movie.getPrimaryTitle() != null)
        .filter(movie -> movie.getPrimaryTitle().startsWith(TEST_MOVIE_PREFIX))
        .forEach(
            movie -> {
              try {
                movieSearchDocumentRepository.deleteById(movie.getId());
              } catch (NoSuchIndexException ignored) {
                // The search index is created lazily by tests that project documents.
              }
              movieRepository.delete(movie);
            });
  }

  @Test
  void getMovieById_badRequest() {
    // Arrange
    var invalidIdFormat = "abc";

    // Act and Assert
    restTestClient
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
    restTestClient
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
    restTestClient
            .get()
            .uri("/api/movie/{movieId}", existingMovie)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectAll(
                    spec -> spec.expectStatus().isOk(),
                    spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
                    spec -> spec.expectBody()
                            .jsonPath("$.id").isEqualTo(existingMovie)
                            .jsonPath("$.primaryTitle").isEqualTo("testMovieOnePri")
                            .jsonPath("$.startYear").isEqualTo(2010)
            );
  }

  @Test
  void getMoviesByIds_emptyResponse() {
    // Arrange
    var nonExistingMovies = new MovieIdsRequest(List.of(999999L));

    // Act and Assert
    restTestClient
            .post()
            .uri("/api/movie/get-movies")
            .accept(MediaType.APPLICATION_JSON)
            .body(nonExistingMovies)
            .exchange()
            .expectAll(
                    spec -> spec.expectStatus().isOk(),
                    spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
                    spec -> spec.expectBody()
                            .jsonPath("$.page").isEqualTo(0)
                            .jsonPath("$.number").doesNotExist()
                            .jsonPath("$.pageable").doesNotExist()
                            .jsonPath("$.content").exists()
                            .jsonPath("$.content[0].primaryTitle").doesNotExist()
            );
  }

  @Test
  void getMoviesByIds_success() {
    // Arrange
    var existingMovies = new MovieIdsRequest(List.of(1L, 2L));

    // Act and Assert
    restTestClient
            .post()
            .uri("/api/movie/get-movies")
            .accept(MediaType.APPLICATION_JSON)
            .body(existingMovies)
            .exchange()
            .expectAll(
                    spec -> spec.expectStatus().isOk(),
                    spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
                    spec -> spec.expectBody()
                            .jsonPath("$.page").isEqualTo(0)
                            .jsonPath("$.number").doesNotExist()
                            .jsonPath("$.pageable").doesNotExist()
                            .jsonPath("$.content[0].id").isEqualTo(1)
                            .jsonPath("$.content[0].primaryTitle").isEqualTo("testMovieOnePri")
                            .jsonPath("$.content[1].id").isEqualTo(2)
                            .jsonPath("$.content[1].primaryTitle").isEqualTo("testMovieTwoPri")
            );
  }

  @Test
  void createMovie_success() throws Exception {
    // Arrange
    var movieRequest =
        new MovieRequest(
            TEST_MOVIE_PREFIX + " create",
            TEST_MOVIE_PREFIX + " create original",
            2015,
            2015,
            105,
            Set.of(MovieGenre.ACTION, MovieGenre.THRILLER),
            MovieType.MOVIE,
            false);

    // Act and Assert
    var createdMovie = createMovie(movieRequest);

    assertThat(createdMovie.id()).isNotNull();
    assertThat(createdMovie.primaryTitle()).isEqualTo(TEST_MOVIE_PREFIX + " create");
    assertThat(createdMovie.startYear()).isEqualTo(2015);

    var movieEntity = movieRepository.getMovieById(createdMovie.id());
    assertThat(movieEntity.getPrimaryTitle()).isEqualTo(TEST_MOVIE_PREFIX + " create");
    assertThat(movieEntity.getOriginalTitle()).isEqualTo(TEST_MOVIE_PREFIX + " create original");

    movieSearchProjectionTaskHandler.projectUpsert(createdMovie.id());

    var movieDocument = movieSearchDocumentRepository.findById(createdMovie.id());
    assertThat(movieDocument).isPresent();
    movieDocument.ifPresent(
        r -> assertThat(r.getPrimaryTitle()).isEqualTo(TEST_MOVIE_PREFIX + " create"));
    movieDocument.ifPresent(r -> assertThat(r.getPrimaryTitle()).isNotEqualTo("no movie"));
  }

  @Test
  void updateMovie_success() throws Exception {
    var createdMovie =
        createMovie(
            new MovieRequest(
                TEST_MOVIE_PREFIX + " update",
                TEST_MOVIE_PREFIX + " update original",
                2010,
                2010,
                95,
                Set.of(MovieGenre.DRAMA),
                MovieType.MOVIE,
                false));

    var updateRequest =
        new MovieRequest(
            TEST_MOVIE_PREFIX + " updated",
            TEST_MOVIE_PREFIX + " updated original",
            2020,
            2020,
            111,
            Set.of(MovieGenre.SCI_FI),
            MovieType.TV_MOVIE,
            false);

    mockMvc
        .perform(
            put("/api/movie/{movieId}", createdMovie.id())
                .with(testAdmin())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(createdMovie.id()))
        .andExpect(jsonPath("$.primaryTitle").value(TEST_MOVIE_PREFIX + " updated"))
        .andExpect(jsonPath("$.originalTitle").value(TEST_MOVIE_PREFIX + " updated original"))
        .andExpect(jsonPath("$.startYear").value(2020))
        .andExpect(jsonPath("$.runtimeMinutes").value(111))
        .andExpect(jsonPath("$.movieType").value("TV_MOVIE"));

    var movieEntity = movieRepository.getMovieById(createdMovie.id());
    assertThat(movieEntity.getPrimaryTitle()).isEqualTo(TEST_MOVIE_PREFIX + " updated");
    assertThat(movieEntity.getRuntimeMinutes()).isEqualTo(111);
    assertThat(movieEntity.getMovieType()).isEqualTo(MovieType.TV_MOVIE);

    movieSearchProjectionTaskHandler.projectUpsert(createdMovie.id());

    var movieDocument = movieSearchDocumentRepository.findById(createdMovie.id());
    assertThat(movieDocument).isPresent();
    movieDocument.ifPresent(
        r -> assertThat(r.getPrimaryTitle()).isEqualTo(TEST_MOVIE_PREFIX + " updated"));
  }

  @Test
  void deleteMovie_success() throws Exception {
    var createdMovie =
        createMovie(
            new MovieRequest(
                TEST_MOVIE_PREFIX + " delete",
                TEST_MOVIE_PREFIX + " delete original",
                2012,
                2012,
                102,
                Set.of(MovieGenre.COMEDY),
                MovieType.MOVIE,
                false));

    movieSearchProjectionTaskHandler.projectUpsert(createdMovie.id());
    assertThat(movieSearchDocumentRepository.findById(createdMovie.id())).isPresent();

    mockMvc
        .perform(
            delete("/api/movie/{movieId}", createdMovie.id())
                .with(testAdmin())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    movieSearchProjectionTaskHandler.projectDelete(createdMovie.id());

    assertThat(movieRepository.findById(createdMovie.id())).isEmpty();
    assertThat(movieSearchDocumentRepository.findById(createdMovie.id())).isEmpty();
  }

  private MovieRecord createMovie(MovieRequest movieRequest) throws Exception {
    var response =
        mockMvc
            .perform(
                post("/api/movie/create-movie")
                    .with(testAdmin())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(movieRequest)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(response).isNotNull();
    return objectMapper.readValue(response, MovieRecord.class);
  }
}
// spotless:on
