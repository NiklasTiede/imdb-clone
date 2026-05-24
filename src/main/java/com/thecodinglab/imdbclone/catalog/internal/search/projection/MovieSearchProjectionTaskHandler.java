package com.thecodinglab.imdbclone.catalog.internal.search.projection;

import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentMapper;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentRepository;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchEmbeddingProjector;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MovieSearchProjectionTaskHandler {

  private final MovieRepository movieRepository;
  private final MovieSearchDocumentRepository movieSearchRepository;
  private final MovieSearchDocumentMapper movieSearchDocumentMapper;
  private final MovieSearchEmbeddingProjector movieSearchEmbeddingProjector;

  public MovieSearchProjectionTaskHandler(
      MovieRepository movieRepository,
      MovieSearchDocumentRepository movieSearchRepository,
      MovieSearchDocumentMapper movieSearchDocumentMapper,
      MovieSearchEmbeddingProjector movieSearchEmbeddingProjector) {
    this.movieRepository = movieRepository;
    this.movieSearchRepository = movieSearchRepository;
    this.movieSearchDocumentMapper = movieSearchDocumentMapper;
    this.movieSearchEmbeddingProjector = movieSearchEmbeddingProjector;
  }

  public void projectUpsert(Long movieId) {
    project(MovieSearchProjectionOperation.UPSERT, movieId);
  }

  public void projectDelete(Long movieId) {
    project(MovieSearchProjectionOperation.DELETE, movieId);
  }

  void project(MovieSearchProjectionOperation operation, Long movieId) {
    switch (operation) {
      case UPSERT -> upsertMovieDocument(movieId);
      case DELETE -> movieSearchRepository.deleteById(movieId);
    }
  }

  private void upsertMovieDocument(Long movieId) {
    Optional<Movie> movie = movieRepository.findById(movieId);
    if (movie.isPresent()) {
      MovieSearchDocument document = movieSearchDocumentMapper.toDocument(movie.get());
      movieSearchEmbeddingProjector.addEmbedding(movie.get(), document);
      movieSearchRepository.save(document);
      return;
    }
    movieSearchRepository.deleteById(movieId);
  }
}
