package com.thecodinglab.imdbclone.catalog.web;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchReindexJobResponse;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.catalog.internal.search.MovieSearchService;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchReindexAlreadyRunningException;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchReindexJobs;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import com.thecodinglab.imdbclone.shared.validation.Pagination;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/search")
public class SearchController {

  private final MovieSearchService movieSearchService;
  private final MovieSearchReindexJobs movieSearchReindexJobs;

  public SearchController(
      MovieSearchService movieSearchService, MovieSearchReindexJobs movieSearchReindexJobs) {
    this.movieSearchService = movieSearchService;
    this.movieSearchReindexJobs = movieSearchReindexJobs;
  }

  @PostMapping("/movies")
  public ResponseEntity<PagedResponse<MovieRecord>> search(
      @Valid @RequestBody MovieSearchRequest request,
      @RequestParam(value = "query") @Size(max = 200) String query,
      @RequestParam(defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page") int page,
      @RequestParam(defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size") int size) {
    return new ResponseEntity<>(
        movieSearchService.searchMovies(query, request, page, size), HttpStatus.OK);
  }

  @PostMapping("/movies/reindex")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MovieSearchReindexJobResponse> reindexMovies() {
    try {
      return new ResponseEntity<>(movieSearchReindexJobs.startReindex(), HttpStatus.ACCEPTED);
    } catch (MovieSearchReindexAlreadyRunningException ex) {
      return new ResponseEntity<>(ex.runningJob(), HttpStatus.CONFLICT);
    }
  }

  @GetMapping("/movies/reindex/{jobId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MovieSearchReindexJobResponse> reindexStatus(@PathVariable UUID jobId) {
    return new ResponseEntity<>(movieSearchReindexJobs.getStatus(jobId), HttpStatus.OK);
  }
}
