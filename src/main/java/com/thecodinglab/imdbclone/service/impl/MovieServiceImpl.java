package com.thecodinglab.imdbclone.service.impl;

import static com.thecodinglab.imdbclone.utility.Log.*;
import static net.logstash.logback.argument.StructuredArguments.*;

import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.payload.PagedResponse;
import com.thecodinglab.imdbclone.payload.mapper.MovieMapper;
import com.thecodinglab.imdbclone.payload.movie.MovieRecord;
import com.thecodinglab.imdbclone.payload.movie.MovieRequest;
import com.thecodinglab.imdbclone.repository.MovieElasticSearchRepository;
import com.thecodinglab.imdbclone.repository.MovieRepository;
import com.thecodinglab.imdbclone.repository.MovieSearchDao;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.service.MovieService;
import com.thecodinglab.imdbclone.validation.Pagination;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MovieServiceImpl implements MovieService {

  private static final Logger logger = LoggerFactory.getLogger(MovieServiceImpl.class);

  private final MovieRepository movieRepository;
  private final MovieElasticSearchRepository elasticSearchRepository;
  private final MovieSearchDao movieSearchDao;
  private final MovieMapper movieMapper;

  public MovieServiceImpl(
      final MovieRepository movieRepository,
      MovieElasticSearchRepository elasticSearchRepository,
      MovieSearchDao movieSearchDao,
      MovieMapper movieMapper) {
    this.movieRepository = movieRepository;
    this.elasticSearchRepository = elasticSearchRepository;
    this.movieSearchDao = movieSearchDao;
    this.movieMapper = movieMapper;
  }

  @Override
  public MovieRecord findMovieById(Long movieId) {
    Movie movie = movieRepository.getMovieById(movieId);
    logger.info("Movie with {} was retrieved", kv(MOVIE_ID, movieId));
    return movieMapper.entityToDTO(movie);
  }

  @Override
  public Page<MovieRecord> findMoviesByIds(List<Long> movieIds, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    Pageable pageable = PageRequest.of(page, size);
    Page<Movie> movies = movieRepository.findByIds(movieIds, pageable);
    logger.info(
        "[{}] movies were retrieved from database",
        v(COUNT, movies.getContent().size()),
        kv(MOVIE_IDS, movies.getContent().stream().map(Movie::getId).toList()));
    return movies.map(movieMapper::entityToDTO);
  }

  @Override
  public Movie createMovie(MovieRequest movieRequest, UserPrincipal currentAccount) {
    Movie movie = movieMapper.dtoToEntity(movieRequest);
    return performSave(movie);
  }

  @Override
  public MovieRecord updateMovie(
      Long movieId, MovieRequest movieRequest, UserPrincipal currentAccount) {
    Movie movie = movieRepository.getMovieById(movieId);
    movieMapper.dtoToEntity(movieRequest);
    Movie updatedMovie = performSave(movie);
    return movieMapper.entityToDTO(updatedMovie);
  }

  @Override
  public MessageResponse deleteMovie(Long movieId, UserPrincipal currentAccount) {
    Movie movie = movieRepository.getMovieById(movieId);
    performDelete(movie);
    return new MessageResponse(
        "the movie [" + movie.getPrimaryTitle() + "] was deleted successfully.");
  }

  @Override
  public PagedResponse<Movie> searchMoviesByTitle(String title, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    PagedResponse<Movie> movies = movieSearchDao.findByPrimaryTitleStartsWith(title, page, size);
    logger.info(
        "[{}] movies were retrieved from database",
        v(COUNT, movies.getContent().size()),
        kv(MOVIE_IDS, movies.getContent().stream().map(Movie::getId).toList()));
    return movies;
  }

  public Movie performSave(Movie movie) {
    Movie updatedMovie = movieRepository.save(movie);
    elasticSearchRepository.save(movie);
    logger.info(
        "the movie [{}] with movieId [{}] was created and/or updated from Mysql and ES",
        updatedMovie.getOriginalTitle(),
        v(MOVIE_ID, updatedMovie.getId()));
    return updatedMovie;
  }

  public void performDelete(Movie movie) {
    movieRepository.delete(movie);
    elasticSearchRepository.delete(movie);
    logger.info(
        "the movie [{}] with [{}] was deleted from Mysql and ES",
        movie.getOriginalTitle(),
        kv(MOVIE_ID, movie.getId()));
  }
}
