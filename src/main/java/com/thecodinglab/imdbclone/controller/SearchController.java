package com.thecodinglab.imdbclone.controller;

import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.payload.movie.MovieSearchRequest;
import com.thecodinglab.imdbclone.service.ElasticSearchService;
import com.thecodinglab.imdbclone.validation.Pagination;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
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
  public ResponseEntity<Page<Movie>> search(
      @Valid @RequestBody MovieSearchRequest request,
      @RequestParam(value = "query") @Size(max = 200) String query,
      @RequestParam(defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page") int page,
      @RequestParam(defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size") int size) {
    return new ResponseEntity<>(
        elasticSearchService.searchMovies(query, request, page, size), HttpStatus.OK);
  }
}
