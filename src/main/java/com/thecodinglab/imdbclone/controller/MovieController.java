package com.thecodinglab.imdbclone.controller;

import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.payload.*;
import com.thecodinglab.imdbclone.payload.movie.MovieIdsRequest;
import com.thecodinglab.imdbclone.payload.movie.MovieRecord;
import com.thecodinglab.imdbclone.payload.movie.MovieRequest;
import com.thecodinglab.imdbclone.security.CurrentUser;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.service.MovieService;
import com.thecodinglab.imdbclone.validation.Pagination;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/api/movie")
public class MovieController {

  private final MovieService movieService;

  public MovieController(MovieService movieService) {
    this.movieService = movieService;
  }

  @GetMapping("/{movieId}")
  public ResponseEntity<MovieRecord> getMovieById(@PathVariable Long movieId) {
    return new ResponseEntity<>(movieService.findMovieById(movieId), HttpStatus.OK);
  }

  @PostMapping("/get-movies")
  public ResponseEntity<PagedResponse<MovieRecord>> getMoviesByIds(
      @RequestBody MovieIdsRequest request,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER) Integer page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE) Integer size) {
    return new ResponseEntity<>(
        movieService.findMoviesByIds(request.movieIds(), page, size), HttpStatus.OK);
  }

  @PostMapping("/create-movie")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Movie> createMovie(
      @Valid @RequestBody MovieRequest request,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        movieService.createMovie(request, currentAccount), HttpStatus.CREATED);
  }

  @PutMapping("/{movieId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Movie> updateMovie(
      @PathVariable Long movieId,
      @Valid @RequestBody MovieRequest request,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        movieService.updateMovie(movieId, request, currentAccount), HttpStatus.CREATED);
  }

  @DeleteMapping("/{movieId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> deleteMovieById(
      @PathVariable Long movieId,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(movieService.deleteMovie(movieId, currentAccount), HttpStatus.OK);
  }

  // substring search does not work very well (IndexOutOfBound-Exception too short search)
  // replace later by Elasticsearch!
  @GetMapping("/search/{primaryTitle}")
  public ResponseEntity<PagedResponse<Movie>> searchMoviesByTitle(
      @PathVariable String primaryTitle,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER) Integer page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE) Integer size) {
    return new ResponseEntity<>(
        movieService.searchMoviesByTitle(primaryTitle, page, size), HttpStatus.OK);
  }
}
