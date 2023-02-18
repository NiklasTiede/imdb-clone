package com.thecodinglab.imdbclone.controller;

import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.payload.PagedResponse;
import com.thecodinglab.imdbclone.payload.movie.MovieSearchRequest;
import com.thecodinglab.imdbclone.service.ElasticSearchService;
import com.thecodinglab.imdbclone.validation.Pagination;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping(("/api/search"))
public class SearchController {

  private final ElasticSearchService elasticSearchService;

  public SearchController(ElasticSearchService elasticSearchService) {
    this.elasticSearchService = elasticSearchService;
  }

  @PostMapping("/movies")
  public ResponseEntity<PagedResponse<Movie>> search(
      @Valid @RequestBody MovieSearchRequest request,
      @RequestParam @Size(max = 200) String query,
      @RequestParam(defaultValue = Pagination.DEFAULT_PAGE_NUMBER) int page,
      @RequestParam(defaultValue = Pagination.DEFAULT_PAGE_SIZE) int size) {
    return new ResponseEntity<>(
        elasticSearchService.searchMovies(query, request, page, size), HttpStatus.OK);
  }
}
