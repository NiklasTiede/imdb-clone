package com.example.demo.controller;

import com.example.demo.Payload.MessageResponse;
import com.example.demo.Payload.MovieRecord;
import com.example.demo.service.MovieService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/api/movie")
public class MovieController {

  private final MovieService movieService;

  public MovieController(MovieService movieService) {
    this.movieService = movieService;
  }

  // ------------------------------------

  //  @GetMapping("/{movieId}")
  //  public ResponseEntity<MovieRecord> findMovieById(@PathVariable Long movieId) {
  //
  //    MovieRecord movieResponse = movieService.findMovieById(movieId);
  //
  //    return new ResponseEntity<>(movieResponse, HttpStatus.OK);
  //  }

  @GetMapping("/search-by-primary-title/{primaryTitle}")
  public ResponseEntity<List<MovieRecord>> searchMovieByTitle(@PathVariable String primaryTitle) {
    List<MovieRecord> moviesResponse = movieService.findMovieByTitle(primaryTitle);
    return new ResponseEntity<>(moviesResponse, HttpStatus.OK);
  }

  @PostMapping("/add")
  //  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> addNewMovie(@RequestBody MovieRecord movieRecord) {
    String savedMovieResponse = movieService.saveMovie(movieRecord);
    return new ResponseEntity<>(new MessageResponse(savedMovieResponse), HttpStatus.OK);
  }

  @DeleteMapping("/{movieId}")
  public ResponseEntity<String> deleteMovieById(@PathVariable Long movieId) {
    String movieResponse = movieService.deleteMovie(movieId);
    return new ResponseEntity<>(movieResponse, HttpStatus.OK);
  }
}
