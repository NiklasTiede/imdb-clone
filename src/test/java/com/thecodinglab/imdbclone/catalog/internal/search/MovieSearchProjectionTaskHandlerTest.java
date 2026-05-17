package com.thecodinglab.imdbclone.catalog.internal.search;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieElasticSearchRepository;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovieSearchProjectionTaskHandlerTest {

  @Mock private MovieRepository movieRepository;
  @Mock private MovieElasticSearchRepository elasticSearchRepository;

  private MovieSearchProjectionTaskHandler handler;

  @BeforeEach
  void setUp() {
    handler = new MovieSearchProjectionTaskHandler(movieRepository, elasticSearchRepository);
  }

  @Test
  void project_upsertSavesMovieDocumentFromDatabase() {
    Movie movie = movieWithId(45L);
    when(movieRepository.findById(45L)).thenReturn(Optional.of(movie));

    handler.project(MovieSearchProjectionOperation.UPSERT, 45L);

    verify(elasticSearchRepository).save(movie);
    verify(elasticSearchRepository, never()).deleteById(45L);
  }

  @Test
  void project_upsertDeletesMovieDocumentWhenDatabaseMovieIsGone() {
    when(movieRepository.findById(46L)).thenReturn(Optional.empty());

    handler.project(MovieSearchProjectionOperation.UPSERT, 46L);

    verify(elasticSearchRepository).deleteById(46L);
  }

  @Test
  void project_deleteDeletesMovieDocument() {
    handler.project(MovieSearchProjectionOperation.DELETE, 47L);

    InOrder inOrder = inOrder(elasticSearchRepository, movieRepository);
    inOrder.verify(elasticSearchRepository).deleteById(47L);
    verify(movieRepository, never()).findById(47L);
  }

  private Movie movieWithId(Long id) {
    Movie movie = new Movie();
    movie.setId(id);
    return movie;
  }
}
