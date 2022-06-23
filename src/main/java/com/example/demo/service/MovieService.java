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

  public List<MovieDto> getMovies() {
    List<Movie> movies = movieRepository.findAll();
    log.info("Movie data were retrieved: " + movies.subList(0, 3));
    return movies.stream().map(this::convertToDto).collect(Collectors.toList());
  }

  public MovieDto findMovieById(Integer movieId) {
    Movie movie =
        movieRepository
            .findById(movieId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Movie with MovieId [" + movieId + "] not found in database."));
    log.info("a movie was retrieved: " + movie);
    return convertToDto(movie);
  }

  private MovieDto convertToDto(Movie movie) {
    return modelMapper.map(movie, MovieDto.class);
  }
}
