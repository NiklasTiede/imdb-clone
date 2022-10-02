package com.example.demo.service.impl;

import com.example.demo.Payload.*;
import com.example.demo.Payload.mapper.MovieMapper;
import com.example.demo.entity.Movie;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.exceptions.UnauthorizedException;
import com.example.demo.repository.MovieRepository;
import com.example.demo.repository.MovieSearchDao;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.MovieService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MovieServiceImpl implements MovieService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MovieServiceImpl.class);

  private final MovieRepository movieRepository;
  private final MovieSearchDao movieSearchDao;
  private final MovieMapper movieMapper;

  public MovieServiceImpl(
      final MovieRepository movieRepository,
      MovieSearchDao movieSearchDao,
      MovieMapper movieMapper) {
    this.movieRepository = movieRepository;
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
  public List<MovieRecord> findMoviesByIds(List<Long> movieIds) {
    List<Movie> movies = movieRepository.findAllById(movieIds);
    if (movies.isEmpty()) {
      throw new NotFoundException("Movies with Ids [" + movieIds + "] not found in database.");
    }
    LOGGER.info("[{}] movies were retrieved from database.", movies.size());
    return movieMapper.entityToDTO(movies);
  }

  @Override
  public Movie createMovie(MovieRequest movieRequest, UserPrincipal currentAccount) {
    if (UserPrincipal.isCurrentAccountAdmin(currentAccount)) {
      Movie movie = movieMapper.dtoToEntity(movieRequest);
      Movie savedMovie = movieRepository.save(movie);
      LOGGER.info(
          "the movie [{}] was saved successfully with movieId [{}].",
          movie.getOriginalTitle(),
          movie.getId());
      return savedMovie;
    } else {
      throw new UnauthorizedException(
          "Account with id ["
              + currentAccount.getId()
              + "] has no permission to create this resource.");
    }
  }

  @Override
  public Movie updateMovie(Long movieId, MovieRequest request, UserPrincipal currentAccount) {
    if (UserPrincipal.isCurrentAccountAdmin(currentAccount)) {
      Movie movie = movieRepository.getMovieById(movieId);
      movie.setPrimaryTitle(request.primaryTitle());
      movie.setOriginalTitle(request.originalTitle());
      movie.setStartYear(request.startYear());
      movie.setEndYear(request.endYear());
      movie.setRuntimeMinutes(request.runtimeMinutes());
      movie.setMovieGenre(request.movieGenre());
      movie.setMovieType(request.movieType());
      movie.setAdult(request.adult());
      Movie updatedMovie = movieRepository.save(movie);
      LOGGER.info(
          "the movie [{}] was updated successfully with movieId [{}].",
          updatedMovie.getOriginalTitle(),
          updatedMovie.getId());
      return updatedMovie;
    } else {
      throw new UnauthorizedException(
          "Account with id ["
              + currentAccount.getId()
              + "] has no permission to update this resource.");
    }
  }

  @Override
  public MessageResponse deleteMovie(Long movieId, UserPrincipal currentAccount) {
    if (UserPrincipal.isCurrentAccountAdmin(currentAccount)) {
      Movie movie = movieRepository.getMovieById(movieId);
      movieRepository.delete(movie);
      return new MessageResponse(
          "the movie [" + movie.getPrimaryTitle() + "] was deleted successfully.");
    } else {
      throw new UnauthorizedException(
          "Account with id ["
              + currentAccount.getId()
              + "] has no permission to delete this resource.");
    }
  }

  @Override
  public List<MovieRecord> searchMoviesByTitle(String title) {

    // PagedResponse

    // https://www.baeldung.com/spring-data-jpa-pagination-sorting

    // https://www.baeldung.com/jpa-pagination

    List<Movie> movies = movieSearchDao.findByPrimaryTitleStartsWith(title);
    if (movies.isEmpty()) {
      throw new NotFoundException("No similar movie with title [" + title + "] found in database.");
    }
    LOGGER.info("[{}] movies were retrieved from database.", movies.size());
    return movieMapper.entityToDTO(movies);
  }

  //  @Override
  //  public PagedResponse<Comment> getAllComments(Long postId, int page, int size) {
  //    AppUtils.validatePageNumberAndSize(page, size);
  //    Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
  //
  //    Page<Comment> comments = commentRepository.findByPostId(postId, pageable);
  //
  //    return new PagedResponse<>(comments.getContent(), comments.getNumber(), comments.getSize(),
  //            comments.getTotalElements(), comments.getTotalPages(), comments.isLast());
  //  }

}
