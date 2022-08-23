package com.example.demo.service;

import com.example.demo.dto.MovieDto;
import com.example.demo.entity.Movie;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.repository.MovieRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MovieService {

  private static final Logger log = LoggerFactory.getLogger(MovieService.class);
  private final MovieRepository movieRepository;
  private final ModelMapper modelMapper;

  public MovieService(final MovieRepository movieRepository, final ModelMapper modelMapper) {
    this.movieRepository = movieRepository;
    this.modelMapper = modelMapper;
  }

  public MovieDto findMovieById(Long movieId) {
    Movie movie =
        movieRepository
            .findById(movieId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Movie with MovieId [" + movieId + "] not found in database."));
    log.info("A movie was retrieved: " + movie);
    return convertToDto(movie);
  }

  public List<MovieDto> findMovieByTitle(String title) throws NotFoundException {
    //    List<Movie> movies = movieRepository.findByTitleContaining(title);
    List<Movie> movies = movieRepository.findByPrimaryTitleContaining(title);
    if (movies.isEmpty()) {
      throw new NotFoundException("Movie with title [" + title + "] not found in database.");
    }
    log.info("Movie was retrieved: " + movies);
    return movies.stream().map(this::convertToDto).collect(Collectors.toList());
  }

  public String saveMovie(MovieDto movieDto) {
    Movie movie = convertToEntity(movieDto);
    movie.setId(0L); // to autoincrement new id
    movieRepository.save(movie);
    log.info("Movie was saved: " + movie);
    return "the movie '" + movie.getOriginalTitle() + "' was saved successfully.";
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
    return "the movie '" + movie.getPrimaryTitle() + "' was deleted successfully.";
  }

  private MovieDto convertToDto(Movie movie) {
    return modelMapper.map(movie, MovieDto.class);
  }

  private Movie convertToEntity(MovieDto movie) {
    return modelMapper.map(movie, Movie.class);
  }
}
