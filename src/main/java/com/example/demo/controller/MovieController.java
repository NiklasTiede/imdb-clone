package com.example.demo.controller;

import com.example.demo.dto.MovieDto;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.service.MovieService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/movie")
public class MovieController {

  private final MovieService movieService;

  public MovieController(MovieService movieService) {
    this.movieService = movieService;
  }

  @GetMapping("/all")
  public ResponseEntity<List<MovieDto>> findAllMovies() {
    List<MovieDto> moviesResponse = movieService.findAllMovies();
    if (!moviesResponse.isEmpty()) {
      return new ResponseEntity<>(moviesResponse, HttpStatus.OK);
    } else {
      throw new NotFoundException("No movies could be retrieved from DB");
    }
  }

  @GetMapping("/{movieId}")
  public ResponseEntity<MovieDto> findMovieById(@PathVariable Integer movieId) {
    MovieDto movieResponse = movieService.findMovieById(movieId);
    return new ResponseEntity<>(movieResponse, HttpStatus.OK);
  }

  @GetMapping("/search-title/{title}")
  public ResponseEntity<List<MovieDto>> searchMovieByTitle(@PathVariable String title) {
    List<MovieDto> moviesResponse = movieService.findMovieByTitle(title);
    return new ResponseEntity<>(moviesResponse, HttpStatus.OK);
  }

  @PostMapping("/add")
  public ResponseEntity<String> addNewMovie(@RequestBody MovieDto movieDto) {
    String movieResponse = movieService.saveMovie(movieDto);
    return new ResponseEntity<>(movieResponse, HttpStatus.OK);
  }

  // endpoint for PUT, updating a movie

  @DeleteMapping("/{movieId}")
  public ResponseEntity<String> deleteMovieById(@PathVariable int movieId) {
    String movieResponse = movieService.deleteMovie(movieId);
    return new ResponseEntity<>(movieResponse, HttpStatus.OK);
  }
}
