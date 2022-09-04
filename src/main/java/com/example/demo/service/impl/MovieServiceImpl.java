package com.example.demo.service.impl;

import com.example.demo.Payload.MovieRecord;
import com.example.demo.Payload.mapper.MovieMapper;
import com.example.demo.entity.Movie;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.repository.MovieRepository;
import com.example.demo.service.MovieService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MovieServiceImpl implements MovieService {

  private static final Logger log = LoggerFactory.getLogger(MovieServiceImpl.class);
  private final MovieRepository movieRepository;

  private final MovieMapper movieMapper;

  public MovieServiceImpl(final MovieRepository movieRepository, MovieMapper movieMapper) {
    this.movieRepository = movieRepository;
    this.movieMapper = movieMapper;
  }

  public MovieRecord findMovieById(Long movieId) {
    Movie movie =
        movieRepository
            .findById(movieId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Movie with MovieId [" + movieId + "] not found in database."));
    log.info("Movie with Id [" + movie.getId() + "] was retrieved.");
    return movieMapper.entityToDTO(movie);
  }

  public List<MovieRecord> findMovieByTitle(String title) throws NotFoundException {
    List<Movie> movies = movieRepository.findByPrimaryTitleContaining(title);
    if (movies.isEmpty()) {
      throw new NotFoundException("Movie with title [" + title + "] not found in database.");
    }
    log.info("Movie was retrieved: " + movies);
    return movieMapper.entityToDTO(movies);
  }

  public String saveMovie(MovieRecord movieRecord) {
    Movie movie = movieMapper.dtoToEntity(movieRecord);
    movie.setId(0L); // to autoincrement new id
    movieRepository.save(movie);
    log.info("Movie was saved: " + movie);
    return "the movie [" + movie.getOriginalTitle() + "] was saved successfully.";
  }

  public String deleteMovie(Long movieId) {
    Movie movie =
        movieRepository
            .findById(movieId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Movie with MovieId [" + movieId + "] not found in database."));
    movieRepository.delete(movie);
    return "the movie [" + movie.getPrimaryTitle() + "] was deleted successfully.";
  }
}
