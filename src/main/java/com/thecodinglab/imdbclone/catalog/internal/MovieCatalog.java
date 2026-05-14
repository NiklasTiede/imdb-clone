package com.thecodinglab.imdbclone.catalog.internal;

import static com.thecodinglab.imdbclone.shared.logging.Log.*;
import static net.logstash.logback.argument.StructuredArguments.*;

import com.thecodinglab.imdbclone.catalog.api.MovieImageToken;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.catalog.api.events.MovieDeleted;
import com.thecodinglab.imdbclone.catalog.internal.mapper.MovieMapper;
import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieSearchDao;
import com.thecodinglab.imdbclone.catalog.internal.search.MovieSearchProjectionTasks;
import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import com.thecodinglab.imdbclone.shared.validation.Pagination;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovieCatalog implements MovieService {

  private static final Logger logger = LoggerFactory.getLogger(MovieCatalog.class);

  private final MovieRepository movieRepository;
  private final MovieSearchDao movieSearchDao;
  private final MovieMapper movieMapper;
  private final MovieSearchProjectionTasks movieSearchProjectionTasks;
  private final ApplicationEventPublisher events;

  public MovieCatalog(
      final MovieRepository movieRepository,
      MovieSearchDao movieSearchDao,
      MovieMapper movieMapper,
      MovieSearchProjectionTasks movieSearchProjectionTasks,
      ApplicationEventPublisher events) {
    this.movieRepository = movieRepository;
    this.movieSearchDao = movieSearchDao;
    this.movieMapper = movieMapper;
    this.movieSearchProjectionTasks = movieSearchProjectionTasks;
    this.events = events;
  }

  @Override
  public MovieRecord findMovieById(Long movieId) {
    Movie movie = movieRepository.getMovieById(movieId);
    logger.info("Movie with {} was retrieved", kv(MOVIE_ID, movieId));
    return movieMapper.entityToDTO(movie);
  }

  @Override
  public PagedResponse<MovieRecord> findMoviesByIds(List<Long> movieIds, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    Pageable pageable = PageRequest.of(page, size);
    Page<Movie> movies = movieRepository.findByIdIn(movieIds, pageable);
    logger.info(
        "[{}] movies with movieIds [{}] were retrieved from database",
        v(COUNT, movies.getContent().size()),
        kv(MOVIE_IDS, movies.getContent().stream().map(Movie::getId).toList()));
    return PagedResponse.from(movies.map(movieMapper::entityToDTO));
  }

  @Override
  @Transactional
  public MovieRecord createMovie(MovieRequest movieRequest) {
    Movie movie = movieMapper.dtoToEntity(movieRequest);
    Movie savedMovie = performSave(movie);
    return movieMapper.entityToDTO(savedMovie);
  }

  @Override
  @Transactional
  public MovieRecord updateMovie(Long movieId, MovieRequest movieRequest) {
    Movie movie = movieRepository.getMovieById(movieId);
    movie.setPrimaryTitle(movieRequest.primaryTitle());
    movie.setOriginalTitle(movieRequest.originalTitle());
    movie.setStartYear(movieRequest.startYear());
    movie.setEndYear(movieRequest.endYear());
    movie.setRuntimeMinutes(movieRequest.runtimeMinutes());
    movie.setMovieGenre(movieRequest.movieGenre());
    movie.setMovieType(movieRequest.movieType());
    movie.setAdult(movieRequest.adult());
    Movie updatedMovie = performSave(movie);
    return movieMapper.entityToDTO(updatedMovie);
  }

  @Override
  @Transactional
  public MessageResponse deleteMovie(Long movieId) {
    Movie movie = movieRepository.getMovieById(movieId);
    performDelete(movie);
    return new MessageResponse(
        "the movie [%s] was deleted successfully.".formatted(movie.getPrimaryTitle()));
  }

  @Override
  public PagedResponse<MovieRecord> searchMoviesByTitle(String title, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    PagedResponse<Movie> movies = movieSearchDao.findByPrimaryTitleStartsWith(title, page, size);
    logger.info(
        "[{}] movies with movieIds [{}] were retrieved from database",
        v(COUNT, movies.getContent().size()),
        kv(MOVIE_IDS, movies.getContent().stream().map(Movie::getId).toList()));
    return movies.map(movieMapper::entityToDTO);
  }

  @Override
  @Transactional
  public void applyRatingAggregateDelta(
      Long movieId, BigDecimal ratingSumDelta, int ratingCountDelta) {
    int updatedMovies =
        movieRepository.applyRatingAggregateDelta(movieId, ratingSumDelta, ratingCountDelta);
    if (updatedMovies == 0) {
      movieRepository.getMovieById(movieId);
      throw new IllegalStateException(
          "Movie rating aggregate for movieId [%d] would become inconsistent.".formatted(movieId));
    }
    movieSearchProjectionTasks.enqueueUpsert(movieId);
    logger.info(
        "the rating aggregate of movie with movieId [{}] was updated", v(MOVIE_ID, movieId));
  }

  @Override
  public MovieImageToken getMovieImageToken(Long movieId) {
    Movie movie = movieRepository.getMovieById(movieId);
    return new MovieImageToken(movie.getId(), movie.getImageUrlToken());
  }

  @Override
  @Transactional
  public MovieImageToken updateMovieImageToken(Long movieId, String imageUrlToken) {
    Movie movie = movieRepository.getMovieById(movieId);
    movie.setImageUrlToken(imageUrlToken);
    Movie savedMovie = performSave(movie);
    return new MovieImageToken(savedMovie.getId(), savedMovie.getImageUrlToken());
  }

  @Override
  @Transactional
  public void clearMovieImageToken(Long movieId) {
    Movie movie = movieRepository.getMovieById(movieId);
    movie.setImageUrlToken(null);
    performSave(movie);
  }

  private Movie performSave(Movie movie) {
    Movie updatedMovie = movieRepository.save(movie);
    movieSearchProjectionTasks.enqueueUpsert(updatedMovie.getId());
    logger.info(
        "the movie [{}] with movieId [{}] was created and/or updated in Mysql and scheduled for ES projection",
        updatedMovie.getOriginalTitle(),
        v(MOVIE_ID, updatedMovie.getId()));
    return updatedMovie;
  }

  private void performDelete(Movie movie) {
    String imageUrlToken = movie.getImageUrlToken();
    movieRepository.delete(movie);
    movieSearchProjectionTasks.enqueueDelete(movie.getId());
    events.publishEvent(new MovieDeleted(movie.getId(), imageUrlToken));
    logger.info(
        "the movie [{}] with [{}] was deleted from Mysql and scheduled for ES projection delete",
        movie.getOriginalTitle(),
        kv(MOVIE_ID, movie.getId()));
  }
}
