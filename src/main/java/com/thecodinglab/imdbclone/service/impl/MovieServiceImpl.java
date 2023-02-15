package com.thecodinglab.imdbclone.service.impl;

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
import jakarta.transaction.Transactional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MovieServiceImpl implements MovieService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MovieServiceImpl.class);

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
    LOGGER.info("Movie with Id [" + movie.getId() + "] was retrieved.");
    return movieMapper.entityToDTO(movie);
  }

  @Override
  public PagedResponse<MovieRecord> findMoviesByIds(List<Long> movieIds, int page, int size) {
    Pagination.validatePageNumberAndSize(page, size);
    Pageable pageable = PageRequest.of(page, size);
    Page<Movie> movies = movieRepository.findByIds(movieIds, pageable);
    LOGGER.info("[{}] movies were retrieved from database.", movies.getContent().size());
    Page<MovieRecord> movieRecordPage = movies.map(movieMapper::entityToDTO);
    return new PagedResponse<>(
        movieRecordPage.getContent(),
        movieRecordPage.getNumber(),
        movieRecordPage.getSize(),
        movieRecordPage.getTotalElements(),
        movieRecordPage.getTotalPages(),
        movieRecordPage.isLast());
  }

  @Override
  public Movie createMovie(MovieRequest movieRequest, UserPrincipal currentAccount) {
    Movie movie = movieMapper.dtoToEntity(movieRequest);
    return performSave(movie);
  }

  @Override
  public Movie updateMovie(Long movieId, MovieRequest request, UserPrincipal currentAccount) {
    Movie movie = movieRepository.getMovieById(movieId);
    movie.setPrimaryTitle(request.primaryTitle());
    movie.setOriginalTitle(request.originalTitle());
    movie.setStartYear(request.startYear());
    movie.setEndYear(request.endYear());
    movie.setRuntimeMinutes(request.runtimeMinutes());
    movie.setMovieGenre(request.movieGenre());
    movie.setMovieType(request.movieType());
    movie.setAdult(request.adult());
    return performSave(movie);
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
    LOGGER.info("[{}] movies were retrieved from database.", movies.getContent().size());
    return movies;
  }

  @Transactional
  public Movie performSave(Movie movie) {
    Movie updatedMovie = movieRepository.save(movie);
    elasticSearchRepository.save(movie);
    LOGGER.info(
        "the movie [{}] with movieId [{}] was created and/or updated from Mysql and ES.",
        updatedMovie.getOriginalTitle(),
        updatedMovie.getId());
    return updatedMovie;
  }

  @Transactional
  private void performDelete(Movie movie) {
    movieRepository.delete(movie);
    elasticSearchRepository.delete(movie);
    LOGGER.info(
        "the movie [{}] with movieId [{}] was deleted from Mysql and ES.",
        movie.getOriginalTitle(),
        movie.getId());
  }
}
