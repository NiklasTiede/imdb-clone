package com.example.demo.controller;

import com.example.demo.dto.MovieDto;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.MovieService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/movie")
public class MovieController {

  private final MovieService movieService;

  public MovieController(MovieService movieService) {
    this.movieService = movieService;
  }

  @GetMapping("/bla")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<String> doSomething(@CurrentUser UserPrincipal currentUser) {

    System.out.println("currentUser:");
    System.out.println(currentUser);
    System.out.println(currentUser.getId());
    System.out.println(currentUser.getFirstName());
    System.out.println(currentUser.getEmail());
    System.out.println(currentUser.getAuthorities());
    System.out.println(currentUser.getUsername());

    return new ResponseEntity<>("", HttpStatus.CREATED);
  }

  @GetMapping("/{movieId}")
  public ResponseEntity<MovieDto> findMovieById(@PathVariable Long movieId) {
    MovieDto movieResponse = movieService.findMovieById(movieId);
    return new ResponseEntity<>(movieResponse, HttpStatus.OK);
  }

  @GetMapping("/search-by-primary-title/{primaryTitle}")
  public ResponseEntity<List<MovieDto>> searchMovieByTitle(@PathVariable String primaryTitle) {
    List<MovieDto> moviesResponse = movieService.findMovieByTitle(primaryTitle);
    return new ResponseEntity<>(moviesResponse, HttpStatus.OK);
  }

  @PostMapping("/add")
  public ResponseEntity<String> addNewMovie(@RequestBody MovieDto movieDto) {
    String movieResponse = movieService.saveMovie(movieDto);
    return new ResponseEntity<>(movieResponse, HttpStatus.OK);
  }

  @DeleteMapping("/{movieId}")
  public ResponseEntity<String> deleteMovieById(@PathVariable Long movieId) {
    String movieResponse = movieService.deleteMovie(movieId);
    return new ResponseEntity<>(movieResponse, HttpStatus.OK);
  }
}
