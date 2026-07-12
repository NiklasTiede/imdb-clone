package com.thecodinglab.imdbclone.catalog.internal.search;

import static com.thecodinglab.imdbclone.catalog.internal.search.MovieSearchMetrics.Mode.DISCOVERY;
import static com.thecodinglab.imdbclone.catalog.internal.search.MovieSearchMetrics.Mode.FILTER_ONLY;
import static com.thecodinglab.imdbclone.catalog.internal.search.MovieSearchMetrics.Mode.LEXICAL_FALLBACK;
import static com.thecodinglab.imdbclone.catalog.internal.search.MovieSearchMetrics.Mode.SEMANTIC;
import static com.thecodinglab.imdbclone.catalog.internal.search.MovieSearchMetrics.Mode.TITLE;
import static com.thecodinglab.imdbclone.shared.logging.Log.MOVIE_ID;
import static net.logstash.logback.argument.StructuredArguments.kv;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.catalog.internal.search.embedding.MovieEmbeddingClient;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentMapper;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchEmbeddingTextBuilder;
import com.thecodinglab.imdbclone.catalog.internal.search.query.MovieSearchQueryBuilder;
import com.thecodinglab.imdbclone.catalog.internal.search.query.MovieSearchQueryIntentClassifier;
import com.thecodinglab.imdbclone.catalog.internal.search.query.MovieSearchRankFusion;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import com.thecodinglab.imdbclone.shared.error.OpenSearchOperationException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

// spotless:off
@Service
public class OpenSearchMovieSearchService implements MovieSearchService {

  private static final Logger logger = LoggerFactory.getLogger(OpenSearchMovieSearchService.class);
  private final OpenSearchClient openSearchClient;
  private final MovieSearchDocumentMapper movieSearchDocumentMapper;
  private final MovieEmbeddingClient movieEmbeddingClient;
  private final MovieSearchQueryBuilder movieSearchQueryBuilder;
  private final MovieSearchRankFusion movieSearchRankFusion;
  private final MovieSearchQueryIntentClassifier movieSearchQueryIntentClassifier;
  private final MovieSearchMetrics movieSearchMetrics;
  private static final String MOVIES_INDEX = "movies";
  private static final int MIN_HYBRID_CANDIDATE_SIZE = 100;
  private static final int MAX_HYBRID_CANDIDATE_SIZE = 500;
  private static final double DISCOVERY_LEXICAL_WEIGHT = 0.05;
  private static final double DISCOVERY_SEMANTIC_WEIGHT = 0.95;

  public OpenSearchMovieSearchService(
      OpenSearchClient openSearchClient,
      MovieSearchDocumentMapper movieSearchDocumentMapper,
      MovieEmbeddingClient movieEmbeddingClient,
      MovieSearchQueryBuilder movieSearchQueryBuilder,
      MovieSearchRankFusion movieSearchRankFusion,
      MovieSearchQueryIntentClassifier movieSearchQueryIntentClassifier,
      MovieSearchMetrics movieSearchMetrics) {
    this.openSearchClient = openSearchClient;
    this.movieSearchDocumentMapper = movieSearchDocumentMapper;
    this.movieEmbeddingClient = movieEmbeddingClient;
    this.movieSearchQueryBuilder = movieSearchQueryBuilder;
    this.movieSearchRankFusion = movieSearchRankFusion;
    this.movieSearchQueryIntentClassifier = movieSearchQueryIntentClassifier;
    this.movieSearchMetrics = movieSearchMetrics;
  }

  @Override
  public void indexMovie(MovieSearchDocument movie) {
    IndexResponse indexResponse;
    try {
      indexResponse = openSearchClient
          .index(idx -> idx
              .index(MOVIES_INDEX)
              .id(movie.getId().toString())
              .document(movie));

      if (logger.isInfoEnabled()) {
        logger.info(
                "Document of type Movie with [{}] of index [{}] was [{}].",
                kv(MOVIE_ID, indexResponse.id()),
                indexResponse.index(),
                indexResponse.result().jsonValue()
        );
      }
    } catch (IOException | OpenSearchException ex) {
      logger.error(
          "Document of type movie with [{}] was not indexed successfully.",
          kv(MOVIE_ID, movie.getId()));
      throw new OpenSearchOperationException(
          "Document of type movie with id [%s] was not indexed successfully."
              .formatted(movie.getId()),
          ex);
    }
  }

  @Override
  public void indexMovies(List<MovieSearchDocument> movies) {
    BulkRequest.Builder br = new BulkRequest.Builder();
    for (MovieSearchDocument movie : movies) {
      br.operations(op -> op
          .index(idx -> idx
              .index(MOVIES_INDEX)
              .id(movie.getId().toString())
              .document(movie)));
    }
    BulkResponse result;
    try {
      result = openSearchClient.bulk(br.build());
      logger.info(
          "Number of documents which were [{}] is [{}]",
          result.items().stream()
              .map(BulkResponseItem::result)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet()),
          result.items().size());
    } catch (IOException | OpenSearchException ex) {
      throw new OpenSearchOperationException("Error while indexing a Movie Document in OpenSearch", ex);
    }
    if (result.errors()) {
      logger.error("Bulk had errors");
      for (BulkResponseItem item : result.items()) {
        if (item.error() != null && logger.isInfoEnabled()) {
          logger.error(item.error().reason());
        }
      }
    }
  }

  /**
   * Search Single Document by ID
   */
  @Override
  public MovieSearchDocument getMovieDocumentById(Long movieId) {
    try {
      GetResponse<MovieSearchDocument> response = openSearchClient
          .get(g -> g
                  .index(MOVIES_INDEX)
                  .id(movieId.toString()),
              MovieSearchDocument.class);

      logger.info("Movie document with primaryTitle [{}] was found.", response.source() != null ? response.source().getPrimaryTitle() : null);
      return response.source();
    } catch (IOException | OpenSearchException ex) {
      logger.error("Movie document with [{}] was not found", kv(MOVIE_ID, movieId));
      throw new OpenSearchOperationException("Error while retrieving a Movie Document by ID in OpenSearch", ex);
    }
  }

  /**
   * Search movies by Primary Title
   */
  @Override
  public List<MovieSearchDocument> searchMoviesByPrimaryTitle(String searchText) {
    SearchResponse<MovieSearchDocument> response;
    try {
      response = openSearchClient
          .search(s -> s
                  .index(MOVIES_INDEX)
                  .query(q -> q
                      .match(m -> m
                          .field("primaryTitle")
                          .query(FieldValue.of(searchText)))),
              MovieSearchDocument.class);
    } catch (IOException | OpenSearchException ex) {
      throw new OpenSearchOperationException("Error while searching for a Movie Document by primaryTitle in OpenSearch", ex);
    }
    List<MovieSearchDocument> movies =
        response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();
    logger.info("Document search by primaryTitle gave [{}] results.", movies.size());
    return movies;
  }

  /**
   * Search movies by range of ratings
   */
  @Override
  public List<MovieSearchDocument> searchMoviesByRatingRange(float minRating, float maxRating) {
    try {
      SearchResponse<MovieSearchDocument> response = openSearchClient
          .search(s -> s
                  .index(MOVIES_INDEX)
                  .query(q -> q
                      .range(r -> r
                          .field("imdbRating")
                          .gte(JsonData.of(minRating))
                          .lte(JsonData.of(maxRating)))),
              MovieSearchDocument.class);
      List<MovieSearchDocument> movies =
          response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();
      logger.info("Document search by ratings between [{}] and [{}] gave [{}] results.",minRating, maxRating, movies.size());
      return movies;
    } catch (IOException | OpenSearchException ex) {
      throw new OpenSearchOperationException("Error while searching for a Movie Document by rating range in OpenSearch", ex);
    }
  }

  /**
   * Searches movies using title-aware lexical ranking or semantic discovery, plus filters.
   */
  @Override
  public PagedResponse<MovieRecord> searchMovies(
      String query, MovieSearchRequest request, int page, int size) {
    long startedAt = movieSearchMetrics.start();
    String normalizedQuery = query == null ? "" : query.trim();
    if (!normalizedQuery.isBlank()) {
      return searchMoviesHybrid(normalizedQuery, request, page, size, startedAt);
    }

    BoolQuery boolQuery = buildBoolQuery(query, request);
    SearchResponse<MovieSearchDocument> response;

    SearchRequest searchRequest =
        SearchRequest.of(s -> s
            .index(MOVIES_INDEX)
            .query(q -> q.bool(boolQuery))
            .from(page * size)
            .size(size)
        );

    try {
      response = openSearchClient.search(searchRequest, MovieSearchDocument.class);
    } catch (IOException | OpenSearchException ex) {
      throw new OpenSearchOperationException("error while search was performed", ex);
    }

    PagedResponse<MovieRecord> result = toPagedMovieResponse(response, page, size);
    movieSearchMetrics.record(FILTER_ONLY, result.getTotalElements(), startedAt);
    return result;
  }

  private PagedResponse<MovieRecord> searchMoviesHybrid(
      String query, MovieSearchRequest request, int page, int size, long startedAt) {
    int candidateSize = hybridCandidateSize(page, size);
    SearchRequest lexicalRequest =
        movieSearchQueryBuilder.buildLexicalCandidateSearchRequest(
            MOVIES_INDEX, query, request, candidateSize);

    SearchResponse<MovieSearchDocument> lexicalResponse;
    try {
      lexicalResponse = openSearchClient.search(lexicalRequest, MovieSearchDocument.class);
    } catch (IOException | OpenSearchException ex) {
      throw new OpenSearchOperationException("error while lexical search was performed", ex);
    }

    List<MovieSearchDocument> lexicalResults = documents(lexicalResponse);
    if (movieSearchQueryIntentClassifier.isConfidentTitleQuery(query, lexicalResults)) {
      PagedResponse<MovieRecord> result = toPagedCandidateMovieResponse(
          lexicalResults, page, size, accessibleTotalHits(lexicalResponse, lexicalResults.size()));
      movieSearchMetrics.record(TITLE, result.getTotalElements(), startedAt);
      return result;
    }

    try {
      float[] queryEmbedding = movieEmbeddingClient.embedText(query);
      if (queryEmbedding == null || queryEmbedding.length == 0) {
        logger.warn("Movie query embedding was empty; falling back to lexical results");
        PagedResponse<MovieRecord> result = toPagedCandidateMovieResponse(
            lexicalResults, page, size, accessibleTotalHits(lexicalResponse, lexicalResults.size()));
        movieSearchMetrics.record(LEXICAL_FALLBACK, result.getTotalElements(), startedAt);
        return result;
      }
      SearchRequest semanticRequest =
          movieSearchQueryBuilder.buildSemanticSearchRequest(
              MOVIES_INDEX,
              queryEmbedding,
              request,
              0,
              candidateSize,
              movieEmbeddingClient.modelName(),
              MovieSearchEmbeddingTextBuilder.VERSION);
      List<MovieSearchDocument> semanticResults = searchDocuments(semanticRequest);
      List<MovieSearchDocument> fusedCandidates =
          movieSearchRankFusion.fuse(
              lexicalResults,
              semanticResults,
              0,
              candidateSize,
              DISCOVERY_LEXICAL_WEIGHT,
              DISCOVERY_SEMANTIC_WEIGHT);
      PagedResponse<MovieRecord> result =
          toPagedCandidateMovieResponse(fusedCandidates, page, size, fusedCandidates.size());
      movieSearchMetrics.record(DISCOVERY, result.getTotalElements(), startedAt);
      return result;
    } catch (IOException | RuntimeException ex) {
      logger.warn("Semantic movie search failed; falling back to lexical results", ex);
      PagedResponse<MovieRecord> result = toPagedCandidateMovieResponse(
          lexicalResults, page, size, accessibleTotalHits(lexicalResponse, lexicalResults.size()));
      movieSearchMetrics.record(LEXICAL_FALLBACK, result.getTotalElements(), startedAt);
      return result;
    }
  }

  @Override
  public PagedResponse<MovieRecord> searchMoviesSemantically(
      String query, MovieSearchRequest request, int page, int size) {
    long startedAt = movieSearchMetrics.start();
    String normalizedQuery = query == null ? "" : query.trim();
    if (normalizedQuery.isBlank()) {
      return searchMovies(query, request, page, size);
    }

    float[] queryEmbedding = movieEmbeddingClient.embedText(normalizedQuery);
    SearchRequest searchRequest =
        movieSearchQueryBuilder.buildSemanticSearchRequest(
            MOVIES_INDEX,
            queryEmbedding,
            request,
            page,
            size,
            movieEmbeddingClient.modelName(),
            MovieSearchEmbeddingTextBuilder.VERSION);

    try {
      SearchResponse<MovieSearchDocument> response =
          openSearchClient.search(searchRequest, MovieSearchDocument.class);
      PagedResponse<MovieRecord> result = toPagedMovieResponse(response, page, size);
      movieSearchMetrics.record(SEMANTIC, result.getTotalElements(), startedAt);
      return result;
    } catch (IOException | OpenSearchException ex) {
      throw new OpenSearchOperationException("error while semantic search was performed", ex);
    }
  }

  private PagedResponse<MovieRecord> toPagedMovieResponse(
      SearchResponse<MovieSearchDocument> response, int page, int size) {
    return toPagedMovieResponse(response, page, size, Integer.MAX_VALUE);
  }

  private PagedResponse<MovieRecord> toPagedMovieResponse(
      SearchResponse<MovieSearchDocument> response, int page, int size, int maxTotalElements) {
    int totalHits = (int) (response.hits().total() != null ? response.hits().total().value() : 0);
    List<MovieSearchDocument> movies =
        response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();

    return toPagedMovieResponse(movies, page, size, Math.min(totalHits, maxTotalElements));
  }

  private PagedResponse<MovieRecord> toPagedCandidateMovieResponse(
      List<MovieSearchDocument> movies, int page, int size, int maxTotalElements) {
    int cappedTotalElements = Math.min(movies.size(), maxTotalElements);
    int from = Math.min(page * size, cappedTotalElements);
    int to = Math.min(from + size, cappedTotalElements);
    List<MovieSearchDocument> pageContent = movies.subList(from, to);
    return toPagedMovieResponse(pageContent, page, size, cappedTotalElements);
  }

  private PagedResponse<MovieRecord> toPagedMovieResponse(
      List<MovieSearchDocument> pageContent, int page, int size, int totalHits) {
    Pageable pageable = PageRequest.of(page, size);

    return PagedResponse.from(
        new PageImpl<>(pageContent, pageable, totalHits).map(movieSearchDocumentMapper::toMovieRecord));
  }

  private int accessibleTotalHits(
      SearchResponse<MovieSearchDocument> response, int candidateCount) {
    long totalHits = response.hits().total() == null ? candidateCount : response.hits().total().value();
    return (int) Math.min(totalHits, candidateCount);
  }

  private int hybridCandidateSize(int page, int size) {
    long requestedWindow = (long) (page + 1) * size * 2;
    return (int)
        Math.min(
            MAX_HYBRID_CANDIDATE_SIZE,
            Math.max(MIN_HYBRID_CANDIDATE_SIZE, requestedWindow));
  }

  private List<MovieSearchDocument> searchDocuments(SearchRequest request) throws IOException {
    SearchResponse<MovieSearchDocument> response = openSearchClient.search(request, MovieSearchDocument.class);
    return documents(response);
  }

  private List<MovieSearchDocument> documents(SearchResponse<MovieSearchDocument> response) {
    return response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();
  }

  @Override
  public BoolQuery buildBoolQuery(String query, MovieSearchRequest request) {
    return movieSearchQueryBuilder.buildBoolQuery(query, request);
  }
}
// spotless:on
