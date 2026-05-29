package com.thecodinglab.imdbclone.catalog.internal.search.index;

import static java.util.Map.entry;

import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.opensearch.data.core.OpenSearchOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.stereotype.Service;

@Service
public class MovieSearchIndexMaintenance {

  private static final Logger logger = LoggerFactory.getLogger(MovieSearchIndexMaintenance.class);
  private static final int REINDEX_PAGE_SIZE = 500;
  private static final int EMBEDDING_PROGRESS_INTERVAL = 10;
  private static final int EMBEDDING_DIMENSIONS = 768;
  private static final String SEARCH_AS_YOU_TYPE = "search_as_you_type";
  private static final String KNN_VECTOR = "knn_vector";

  private final MovieRepository movieRepository;
  private final MovieSearchDocumentRepository movieSearchRepository;
  private final MovieSearchDocumentMapper movieSearchDocumentMapper;
  private final MovieSearchEmbeddingProjector movieSearchEmbeddingProjector;
  private final OpenSearchOperations openSearchOperations;

  public MovieSearchIndexMaintenance(
      MovieRepository movieRepository,
      MovieSearchDocumentRepository movieSearchRepository,
      MovieSearchDocumentMapper movieSearchDocumentMapper,
      MovieSearchEmbeddingProjector movieSearchEmbeddingProjector,
      OpenSearchOperations openSearchOperations) {
    this.movieRepository = movieRepository;
    this.movieSearchRepository = movieSearchRepository;
    this.movieSearchDocumentMapper = movieSearchDocumentMapper;
    this.movieSearchEmbeddingProjector = movieSearchEmbeddingProjector;
    this.openSearchOperations = openSearchOperations;
  }

  public long reindexMovies() {
    long reindexStartedAt = System.nanoTime();
    createMoviesIndexIfMissingOrStale();
    movieSearchRepository.deleteAll();

    long indexedMovies = 0;
    int pageNumber = 0;
    Page<Movie> page;
    do {
      page =
          movieRepository.findAll(
              PageRequest.of(pageNumber, REINDEX_PAGE_SIZE, Sort.by("id").ascending()));
      if (page.hasContent()) {
        long batchStartedAt = System.nanoTime();
        long embeddingStartedAt = System.nanoTime();
        List<MovieSearchDocument> documents =
            toEmbeddedDocuments(page.getContent(), pageNumber, indexedMovies);
        long embeddingDurationInMs = elapsedMillisSince(embeddingStartedAt);

        long saveStartedAt = System.nanoTime();
        movieSearchRepository.saveAll(documents);
        long saveDurationInMs = elapsedMillisSince(saveStartedAt);

        indexedMovies += page.getNumberOfElements();
        logger.info(
            "Indexed movie search batch page={} batchSize={} totalIndexed={} embeddingDurationMs={} saveDurationMs={} batchDurationMs={}",
            pageNumber,
            page.getNumberOfElements(),
            indexedMovies,
            embeddingDurationInMs,
            saveDurationInMs,
            elapsedMillisSince(batchStartedAt));
      }
      pageNumber++;
    } while (page.hasNext());

    logger.info(
        "Finished movie search reindex totalIndexed={} durationMs={}",
        indexedMovies,
        elapsedMillisSince(reindexStartedAt));
    return indexedMovies;
  }

  private List<MovieSearchDocument> toEmbeddedDocuments(
      List<Movie> movies, int pageNumber, long alreadyIndexedMovies) {
    List<MovieSearchDocument> documents = new ArrayList<>(movies.size());
    long batchEmbeddingStartedAt = System.nanoTime();
    long embeddingProgressIntervalStartedAt = batchEmbeddingStartedAt;
    int lastLoggedEmbeddedCount = 0;
    for (Movie movie : movies) {
      documents.add(toEmbeddedDocument(movie));
      if (shouldLogEmbeddingProgress(documents.size(), movies.size())) {
        int intervalSize = documents.size() - lastLoggedEmbeddedCount;
        long intervalDurationInMs = elapsedMillisSince(embeddingProgressIntervalStartedAt);
        logger.info(
            "Embedded movie search documents page={} embeddedInPage={} batchSize={} totalEmbedded={} intervalSize={} averageMovieEmbeddingDurationMs={} elapsedMs={}",
            pageNumber,
            documents.size(),
            movies.size(),
            alreadyIndexedMovies + documents.size(),
            intervalSize,
            averageDurationInMs(intervalDurationInMs, intervalSize),
            elapsedMillisSince(batchEmbeddingStartedAt));
        lastLoggedEmbeddedCount = documents.size();
        embeddingProgressIntervalStartedAt = System.nanoTime();
      }
    }
    return documents;
  }

  private static boolean shouldLogEmbeddingProgress(int embeddedInPage, int batchSize) {
    return embeddedInPage == batchSize || embeddedInPage % EMBEDDING_PROGRESS_INTERVAL == 0;
  }

  private static long elapsedMillisSince(long startedAt) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
  }

  private static long averageDurationInMs(long durationInMs, int itemCount) {
    if (itemCount == 0) {
      return 0;
    }
    return durationInMs / itemCount;
  }

  private MovieSearchDocument toEmbeddedDocument(Movie movie) {
    MovieSearchDocument document = movieSearchDocumentMapper.toDocument(movie);
    movieSearchEmbeddingProjector.addEmbedding(movie, document);
    return document;
  }

  private void createMoviesIndexIfMissingOrStale() {
    IndexOperations index = openSearchOperations.indexOps(MovieSearchDocument.class);
    if (!index.exists()) {
      createMoviesIndex(index);
      return;
    }
    if (!hasCurrentMovieMapping(index.getMapping())) {
      index.delete();
      createMoviesIndex(index);
    }
  }

  private void createMoviesIndex(IndexOperations index) {
    index.create(openSearchSettings(), Document.from(openSearchMapping()));
  }

  private Map<String, Object> openSearchSettings() {
    return Map.of("index", Map.of("knn", true, "number_of_shards", 1, "number_of_replicas", 0));
  }

  private Map<String, Object> openSearchMapping() {
    return Map.of(
        "properties",
        Map.ofEntries(
            entry("imdbId", Map.of("type", "keyword")),
            entry("tmdbId", Map.of("type", "long")),
            entry("movieType", Map.of("type", "keyword")),
            entry("primaryTitle", Map.of("type", SEARCH_AS_YOU_TYPE)),
            entry("originalTitle", Map.of("type", SEARCH_AS_YOU_TYPE)),
            entry("adult", Map.of("type", "boolean")),
            entry("startYear", Map.of("type", "integer")),
            entry("endYear", Map.of("type", "integer")),
            entry("runtimeMinutes", Map.of("type", "integer")),
            entry("modifiedAtInUtc", dateMapping()),
            entry("createdAtInUtc", dateMapping()),
            entry("movieGenre", Map.of("type", "keyword")),
            entry("imdbRating", Map.of("type", "float")),
            entry("imdbRatingCount", Map.of("type", "integer")),
            entry("description", Map.of("type", "text")),
            entry("posterImageToken", Map.of("type", "keyword")),
            entry("backdropImageToken", Map.of("type", "keyword")),
            entry("trailerYoutubeKey", Map.of("type", "keyword")),
            entry("rating", Map.of("type", "float")),
            entry("ratingCount", Map.of("type", "integer")),
            entry("embeddingModel", Map.of("type", "keyword")),
            entry("embeddingTextVersion", Map.of("type", "keyword")),
            entry("embedding", embeddingMapping())));
  }

  private Map<String, Object> dateMapping() {
    return Map.of("type", "date", "format", "strict_date_optional_time||epoch_millis");
  }

  private Map<String, Object> embeddingMapping() {
    return Map.of(
        "type",
        KNN_VECTOR,
        "dimension",
        EMBEDDING_DIMENSIONS,
        "method",
        Map.of("name", "hnsw", "engine", "lucene", "space_type", "cosinesimil"));
  }

  private boolean hasCurrentMovieMapping(Map<String, Object> mapping) {
    return hasSearchAsYouTypeMapping(mapping, "primaryTitle")
        && hasSearchAsYouTypeMapping(mapping, "originalTitle")
        && hasKnnVectorEmbeddingMapping(mapping);
  }

  private boolean hasSearchAsYouTypeMapping(Map<String, Object> mapping, String propertyName) {
    if (!(mapping.get("properties") instanceof Map<?, ?> properties)) {
      return false;
    }
    if (!(properties.get(propertyName) instanceof Map<?, ?> property)) {
      return false;
    }
    return SEARCH_AS_YOU_TYPE.equals(property.get("type"));
  }

  private boolean hasKnnVectorEmbeddingMapping(Map<String, Object> mapping) {
    if (!(mapping.get("properties") instanceof Map<?, ?> properties)) {
      return false;
    }
    if (!(properties.get("embedding") instanceof Map<?, ?> property)) {
      return false;
    }
    return KNN_VECTOR.equals(property.get("type"));
  }
}
