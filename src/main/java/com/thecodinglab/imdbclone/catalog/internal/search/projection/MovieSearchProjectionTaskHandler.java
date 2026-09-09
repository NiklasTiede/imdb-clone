package com.thecodinglab.imdbclone.catalog.internal.search.projection;

import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentMapper;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentRepository;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchEmbeddingProjector;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MovieSearchProjectionTaskHandler {

  private static final Logger log = LoggerFactory.getLogger(MovieSearchProjectionTaskHandler.class);
  private final MovieSearchProjectionWork work;
  private final MeterRegistry meters;
  private final MovieRepository movieRepository;
  private final MovieSearchDocumentRepository movieSearchRepository;
  private final MovieSearchDocumentMapper movieSearchDocumentMapper;
  private final MovieSearchEmbeddingProjector movieSearchEmbeddingProjector;

  public MovieSearchProjectionTaskHandler(
      MovieRepository movieRepository,
      MovieSearchDocumentRepository movieSearchRepository,
      MovieSearchDocumentMapper movieSearchDocumentMapper,
      MovieSearchEmbeddingProjector movieSearchEmbeddingProjector,
      MovieSearchProjectionWork work,
      MeterRegistry meters) {
    this.work = work;
    this.meters = meters;
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

  public boolean projectForReindex(Long movieId) {
    return project(MovieSearchProjectionOperation.UPSERT, movieId);
  }

  boolean project(MovieSearchProjectionOperation operation, Long movieId) {
    long revision = work.begin(movieId);
    try {
      // Persisted operations are wake-up hints; PostgreSQL is authoritative even for old/replayed
      // tasks.
      upsertMovieDocument(movieId);
    } catch (RuntimeException exception) {
      work.failed(movieId, revision);
      meters.counter("catalog.projection.failures").increment();
      log.warn("Movie projection failed; persistent retry retained");
      return false;
    }
    work.completed(movieId, revision);
    meters.counter("catalog.projection.completed").increment();
    return true;
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
