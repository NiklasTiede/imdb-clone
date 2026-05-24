package com.thecodinglab.imdbclone.catalog.internal;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.catalog.api.events.MovieDeleted;
import com.thecodinglab.imdbclone.catalog.internal.mapper.MovieMapper;
import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieSearchDao;
import com.thecodinglab.imdbclone.catalog.internal.search.projection.MovieSearchProjectionTasks;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MovieCatalogTest {

  @Mock private MovieRepository movieRepository;
  @Mock private MovieSearchDao movieSearchDao;
  @Mock private MovieMapper movieMapper;
  @Mock private MovieSearchProjectionTasks movieSearchProjectionTasks;
  @Mock private ApplicationEventPublisher events;

  private MovieCatalog movieCatalog;

  @BeforeEach
  void setUp() {
    movieCatalog =
        new MovieCatalog(
            movieRepository, movieSearchDao, movieMapper, movieSearchProjectionTasks, events);
  }

  @Test
  void createMovie_savesMovieAndEnqueuesSearchUpsert() {
    MovieRequest request =
        new MovieRequest(
            "Projection",
            "Projection original",
            2020,
            2020,
            90,
            Set.of(MovieGenre.DRAMA),
            MovieType.MOVIE,
            false);
    Movie movie = new Movie();
    Movie savedMovie = movieWithId(42L);
    when(movieMapper.dtoToEntity(request)).thenReturn(movie);
    when(movieRepository.save(movie)).thenReturn(savedMovie);

    movieCatalog.createMovie(request);

    verify(movieSearchProjectionTasks).enqueueUpsert(42L);
  }

  @Test
  void deleteMovie_deletesMovieAndEnqueuesSearchDelete() {
    Movie movie = movieWithId(43L);
    movie.setImageUrlToken("movie-image-token");
    when(movieRepository.getMovieById(43L)).thenReturn(movie);

    movieCatalog.deleteMovie(43L);

    InOrder inOrder = inOrder(movieRepository, movieSearchProjectionTasks);
    inOrder.verify(movieRepository).delete(movie);
    inOrder.verify(movieSearchProjectionTasks).enqueueDelete(43L);
    verify(events).publishEvent(new MovieDeleted(43L, "movie-image-token"));
  }

  @Test
  void applyRatingAggregateDelta_enqueuesSearchUpsert() {
    when(movieRepository.applyRatingAggregateDelta(44L, BigDecimal.ONE, 1)).thenReturn(1);

    movieCatalog.applyRatingAggregateDelta(44L, BigDecimal.ONE, 1);

    verify(movieSearchProjectionTasks).enqueueUpsert(44L);
  }

  private Movie movieWithId(Long id) {
    Movie movie = new Movie();
    movie.setId(id);
    movie.setOriginalTitle("Projection original");
    return movie;
  }
}
