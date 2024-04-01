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
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movie")
public class MovieController {

  private final MovieService movieService;

  public MovieController(MovieService movieService) {
    this.movieService = movieService;
  }

  @GetMapping("/{movieId}")
  public ResponseEntity<MovieRecord> getMovieById(@PathVariable("movieId") Long movieId) {
    return new ResponseEntity<>(movieService.findMovieById(movieId), HttpStatus.OK);
  }

  @PostMapping("/get-movies")
  public ResponseEntity<Page<MovieRecord>> getMoviesByIds(
      @RequestBody MovieIdsRequest request,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page")
          int page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size")
          int size) {
    Pagination.validatePageNumberAndSize(page, size);
    return new ResponseEntity<>(
        movieService.findMoviesByIds(request.movieIds(), page, size), HttpStatus.OK);
  }

  @PostMapping("/create-movie")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MovieRecord> createMovie(
      @Valid @RequestBody MovieRequest request,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        movieService.createMovie(request, currentAccount), HttpStatus.CREATED);
  }

  @PutMapping("/{movieId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MovieRecord> updateMovie(
      @PathVariable("movieId") Long movieId,
      @Valid @RequestBody MovieRequest request,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        movieService.updateMovie(movieId, request, currentAccount), HttpStatus.OK);
  }

  @DeleteMapping("/{movieId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> deleteMovie(
      @PathVariable("movieId") Long movieId,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        movieService.deleteMovie(movieId, currentAccount), HttpStatus.NO_CONTENT);
  }

  /**
   * @deprecated and thus replaced by ElasticsSearch Queries
   */
  @Deprecated(forRemoval = true)
  @GetMapping("/search/{primaryTitle}")
  public ResponseEntity<PagedResponse<Movie>> searchMoviesByTitle(
      @PathVariable("primaryTitle") String primaryTitle,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page")
          int page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size")
          int size) {
    return new ResponseEntity<>(
        movieService.searchMoviesByTitle(primaryTitle, page, size), HttpStatus.OK);
  }
}
