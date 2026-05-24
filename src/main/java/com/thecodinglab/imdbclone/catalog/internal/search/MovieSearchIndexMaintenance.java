package com.thecodinglab.imdbclone.catalog.internal.search;

import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

@Service
public class MovieSearchIndexMaintenance {

  private static final int REINDEX_PAGE_SIZE = 500;

  private final MovieRepository movieRepository;
  private final MovieSearchDocumentRepository movieSearchRepository;
  private final MovieSearchDocumentMapper movieSearchDocumentMapper;
  private final ElasticsearchOperations elasticsearchOperations;

  public MovieSearchIndexMaintenance(
      MovieRepository movieRepository,
      MovieSearchDocumentRepository movieSearchRepository,
      MovieSearchDocumentMapper movieSearchDocumentMapper,
      ElasticsearchOperations elasticsearchOperations) {
    this.movieRepository = movieRepository;
    this.movieSearchRepository = movieSearchRepository;
    this.movieSearchDocumentMapper = movieSearchDocumentMapper;
    this.elasticsearchOperations = elasticsearchOperations;
  }

  public long reindexMovies() {
    createMoviesIndexIfMissing();
    movieSearchRepository.deleteAll();

    long indexedMovies = 0;
    int pageNumber = 0;
    Page<Movie> page;
    do {
      page =
          movieRepository.findAll(
              PageRequest.of(pageNumber, REINDEX_PAGE_SIZE, Sort.by("id").ascending()));
      if (page.hasContent()) {
        List<MovieSearchDocument> documents =
            page.getContent().stream().map(movieSearchDocumentMapper::toDocument).toList();
        movieSearchRepository.saveAll(documents);
        indexedMovies += page.getNumberOfElements();
      }
      pageNumber++;
    } while (page.hasNext());

    return indexedMovies;
  }

  private void createMoviesIndexIfMissing() {
    IndexOperations index = elasticsearchOperations.indexOps(MovieSearchDocument.class);
    if (!index.exists()) {
      index.createWithMapping();
    }
  }
}
