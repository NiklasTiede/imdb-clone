package com.thecodinglab.imdbclone.catalog.web;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import com.thecodinglab.imdbclone.shared.validation.Pagination;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Validated
@RequestMapping(path = "/api/v{version}/movies", version = "1")
public class MovieController {

  private final MovieService movieService;

  public MovieController(MovieService movieService) {
    this.movieService = movieService;
  }

  @GetMapping("/{movieId}")
  public ResponseEntity<MovieRecord> getMovieById(@PathVariable Long movieId) {
    return new ResponseEntity<>(movieService.findMovieById(movieId), HttpStatus.OK);
  }

  @GetMapping
  public ResponseEntity<PagedResponse<MovieRecord>> getMoviesByIds(
      @RequestParam("ids") @Size(min = 1, max = 30) List<@Positive Long> ids,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page")
          int page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size")
          int size) {
    Pagination.validatePageNumberAndSize(page, size);
    return new ResponseEntity<>(movieService.findMoviesByIds(ids, page, size), HttpStatus.OK);
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<MovieRecord> createMovie(@Valid @RequestBody MovieRequest request) {
    MovieRecord movie = movieService.createMovie(request);
    return ResponseEntity.created(
            ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(movie.id())
                .toUri())
        .body(movie);
  }

  @PutMapping("/{movieId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MovieRecord> updateMovie(
      @PathVariable Long movieId, @Valid @RequestBody MovieRequest request) {
    return new ResponseEntity<>(movieService.updateMovie(movieId, request), HttpStatus.OK);
  }

  @DeleteMapping("/{movieId}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> deleteMovie(@PathVariable Long movieId) {
    movieService.deleteMovie(movieId);
    return ResponseEntity.noContent().build();
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
