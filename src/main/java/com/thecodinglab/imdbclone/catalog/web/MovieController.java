package com.thecodinglab.imdbclone.catalog.web;

import com.thecodinglab.imdbclone.catalog.api.MovieIdsRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.payload.*;
import com.thecodinglab.imdbclone.validation.Pagination;
import jakarta.validation.Valid;
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
  public ResponseEntity<MovieRecord> getMovieById(@PathVariable Long movieId) {
    return new ResponseEntity<>(movieService.findMovieById(movieId), HttpStatus.OK);
  }

  @PostMapping("/get-movies")
  public ResponseEntity<PagedResponse<MovieRecord>> getMoviesByIds(
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
  public ResponseEntity<MovieRecord> createMovie(@Valid @RequestBody MovieRequest request) {
    return new ResponseEntity<>(movieService.createMovie(request), HttpStatus.CREATED);
  }

  @PutMapping("/{movieId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MovieRecord> updateMovie(
      @PathVariable Long movieId, @Valid @RequestBody MovieRequest request) {
    return new ResponseEntity<>(movieService.updateMovie(movieId, request), HttpStatus.OK);
  }

  @DeleteMapping("/{movieId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> deleteMovie(@PathVariable Long movieId) {
    return new ResponseEntity<>(movieService.deleteMovie(movieId), HttpStatus.NO_CONTENT);
  }

  /**
   * @deprecated and thus replaced by ElasticsSearch Queries
   */
  @Deprecated(forRemoval = true)
  @GetMapping("/search/{primaryTitle}")
  public ResponseEntity<PagedResponse<MovieRecord>> searchMoviesByTitle(
      @PathVariable String primaryTitle,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page")
          int page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size")
          int size) {
    return new ResponseEntity<>(
        movieService.searchMoviesByTitle(primaryTitle, page, size), HttpStatus.OK);
  }
}
