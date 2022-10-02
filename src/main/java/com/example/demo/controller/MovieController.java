package com.example.demo.controller;

import com.example.demo.Payload.*;
import com.example.demo.entity.Movie;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.CommentService;
import com.example.demo.service.MovieService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/api/movie")
public class MovieController {

  private final MovieService movieService;
  private final CommentService commentService;

  public MovieController(MovieService movieService, CommentService commentService) {
    this.movieService = movieService;
    this.commentService = commentService;
  }

  @GetMapping("/{movieId}")
  public ResponseEntity<MovieRecord> findMovieById(@PathVariable Long movieId) {
    return new ResponseEntity<>(movieService.findMovieById(movieId), HttpStatus.OK);
  }

  @PostMapping("/get-movies")
  public ResponseEntity<List<MovieRecord>> findMoviesByIds(@RequestBody MovieIdsRequest request) {
    return new ResponseEntity<>(movieService.findMoviesByIds(request.movieIds()), HttpStatus.OK);
  }

  // introduce pages response? let everyone access ratings of others as well
  @GetMapping("/{movieId}/comments")
  public ResponseEntity<List<CommentRecord>> findCommentsByMovieId(@PathVariable Long movieId) {
    return new ResponseEntity<>(commentService.getCommentsByMovieId(movieId), HttpStatus.OK);
  }

  @PostMapping("/create-movie")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Movie> createMovie(
      @RequestBody MovieRequest request, @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        movieService.createMovie(request, currentAccount), HttpStatus.CREATED);
  }

  @PutMapping("/{movieId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Movie> updateMovie(
      @PathVariable Long movieId,
      @RequestBody MovieRequest request,
      @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        movieService.updateMovie(movieId, request, currentAccount), HttpStatus.CREATED);
  }

  @DeleteMapping("/{movieId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> deleteMovieById(
      @PathVariable Long movieId, @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(movieService.deleteMovie(movieId, currentAccount), HttpStatus.OK);
  }

  // introduce pages response?
  @GetMapping("/search-by-primary-title/{primaryTitle}")
  public ResponseEntity<List<MovieRecord>> searchMoviesByTitle(@PathVariable String primaryTitle) {
    return new ResponseEntity<>(movieService.searchMoviesByTitle(primaryTitle), HttpStatus.OK);
  }
}
