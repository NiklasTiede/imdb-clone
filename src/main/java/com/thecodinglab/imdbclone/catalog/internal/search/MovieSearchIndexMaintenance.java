package com.thecodinglab.imdbclone.catalog.internal.search;

import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieElasticSearchRepository;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class MovieSearchIndexMaintenance {

  private static final int REINDEX_PAGE_SIZE = 500;

  private final MovieRepository movieRepository;
  private final MovieElasticSearchRepository movieSearchRepository;

  public MovieSearchIndexMaintenance(
      MovieRepository movieRepository, MovieElasticSearchRepository movieSearchRepository) {
    this.movieRepository = movieRepository;
    this.movieSearchRepository = movieSearchRepository;
  }

  public long reindexMovies() {
    movieSearchRepository.deleteAll();

    long indexedMovies = 0;
    int pageNumber = 0;
    Page<Movie> page;
    do {
      page =
          movieRepository.findAll(
              PageRequest.of(pageNumber, REINDEX_PAGE_SIZE, Sort.by("id").ascending()));
      if (page.hasContent()) {
        movieSearchRepository.saveAll(page.getContent());
        indexedMovies += page.getNumberOfElements();
      }
      pageNumber++;
    } while (page.hasNext());

    return indexedMovies;
  }
}
