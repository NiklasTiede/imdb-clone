package com.thecodinglab.imdbclone.catalog.internal.search;

import static com.thecodinglab.imdbclone.shared.logging.Log.MOVIE_ID;
import static net.logstash.logback.argument.StructuredArguments.kv;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.catalog.internal.search.embedding.MovieEmbeddingClient;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentMapper;
import com.thecodinglab.imdbclone.catalog.internal.search.query.MovieSearchQueryBuilder;
import com.thecodinglab.imdbclone.catalog.internal.search.query.MovieSearchRankFusion;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import com.thecodinglab.imdbclone.shared.error.ElasticsearchOperationException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

// spotless:off
@Service
public class ElasticsearchMovieSearchService implements MovieSearchService {

  private static final Logger logger = LoggerFactory.getLogger(ElasticsearchMovieSearchService.class);
  private final ElasticsearchClient esClient;
  private final MovieSearchDocumentMapper movieSearchDocumentMapper;
  private final MovieEmbeddingClient movieEmbeddingClient;
  private final MovieSearchQueryBuilder movieSearchQueryBuilder;
  private final MovieSearchRankFusion movieSearchRankFusion;
  private static final String MOVIES_INDEX = "movies";
  private static final int HYBRID_CANDIDATE_SIZE = 100;

  public ElasticsearchMovieSearchService(
      ElasticsearchClient esClient,
      MovieSearchDocumentMapper movieSearchDocumentMapper,
      MovieEmbeddingClient movieEmbeddingClient,
      MovieSearchQueryBuilder movieSearchQueryBuilder,
      MovieSearchRankFusion movieSearchRankFusion) {
    this.esClient = esClient;
    this.movieSearchDocumentMapper = movieSearchDocumentMapper;
    this.movieEmbeddingClient = movieEmbeddingClient;
    this.movieSearchQueryBuilder = movieSearchQueryBuilder;
    this.movieSearchRankFusion = movieSearchRankFusion;
  }

  @Override
  public void indexMovie(MovieSearchDocument movie) {
    IndexResponse indexResponse;
    try {
      indexResponse = esClient
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
    } catch (IOException ex) {
      logger.error(
          "Document of type movie with [{}] was not indexed successfully.",
          kv(MOVIE_ID, movie.getId()));
      throw new ElasticsearchOperationException(
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
      result = esClient.bulk(br.build());
      logger.info(
          "Number of documents which were [{}] is [{}]",
          result.items().stream()
              .map(BulkResponseItem::result)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet()),
          result.items().size());
    } catch (IOException ex) {
      throw new ElasticsearchOperationException("Error while indexing a Movie Document in ElasticSearch", ex);
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
  public MovieSearchDocument getMovieDocumentById(Long movieId) {
    try {
      GetResponse<MovieSearchDocument> response = esClient
          .get(g -> g
                  .index(MOVIES_INDEX)
                  .id(movieId.toString()),
              MovieSearchDocument.class);

      logger.info("Movie document with primaryTitle [{}] was found.", response.source() != null ? response.source().getPrimaryTitle() : null);
      return response.source();
    } catch (IOException ex) {
      logger.error("Movie document with [{}] was not found", kv(MOVIE_ID, movieId));
      throw new ElasticsearchOperationException("Error while retrieving a Movie Document by ID in ElasticSearch", ex);
    }
  }

  /**
   * Search movies by Primary Title
   */
  public List<MovieSearchDocument> searchMoviesByPrimaryTitle(String searchText) {
    SearchResponse<MovieSearchDocument> response;
    try {
      response = esClient
          .search(s -> s
                  .index(MOVIES_INDEX)
                  .query(q -> q
                      .match(m -> m
                          .field("primaryTitle")
                          .query(searchText))),
              MovieSearchDocument.class);
    } catch (IOException ex) {
      throw new ElasticsearchOperationException("Error while searching for a Movie Document by primaryTitle in ElasticSearch", ex);
    }
    List<MovieSearchDocument> movies =
        response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();
    logger.info("Document search by primaryTitle gave [{}] results.", movies.size());
    return movies;
  }

  /**
   * Search movies by range of ratings
   */
  public List<MovieSearchDocument> searchMoviesByRatingRange(float minRating, float maxRating) {
    try {
      SearchResponse<MovieSearchDocument> response = esClient
          .search(s -> s
                  .index(MOVIES_INDEX)
                  .query(q -> q
                      .range(r -> r
                          .number(n -> n
                              .field("imdbRating")
                              .gte((double) minRating)
                              .lte((double) maxRating)))),
              MovieSearchDocument.class);
      List<MovieSearchDocument> movies =
          response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();
      logger.info("Document search by ratings between [{}] and [{}] gave [{}] results.",minRating, maxRating, movies.size());
      return movies;
    } catch (IOException ex) {
      throw new ElasticsearchOperationException("Error while searching for a Movie Document by rating range in ElasticSearch", ex);
    }
  }

  /**
   * Search Movies by Multiple Parameters, highest voted movies scoring is boosted
   */
  @Override
  public PagedResponse<MovieRecord> searchMovies(
      String query, MovieSearchRequest request, int page, int size) {
    String normalizedQuery = query == null ? "" : query.trim();
    if (!normalizedQuery.isBlank()) {
      return searchMoviesHybrid(normalizedQuery, request, page, size);
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
    logger.info("Document search query json: [{}]", searchRequest);

    try {
      response = esClient.search(searchRequest, MovieSearchDocument.class);
    } catch (IOException ex) {
      throw new ElasticsearchOperationException("error while search was performed", ex);
    }
    logger.info(
        "Scores of found documents: [{}]",
        response.hits().hits().stream().map(Hit::score).toList());

    return toPagedMovieResponse(response, page, size);
  }

  private PagedResponse<MovieRecord> searchMoviesHybrid(
      String query, MovieSearchRequest request, int page, int size) {
    SearchRequest lexicalRequest =
        movieSearchQueryBuilder.buildLexicalCandidateSearchRequest(
            MOVIES_INDEX, query, request, HYBRID_CANDIDATE_SIZE);
    float[] queryEmbedding = movieEmbeddingClient.embedText(query);
    SearchRequest semanticRequest =
        movieSearchQueryBuilder.buildSemanticSearchRequest(
            MOVIES_INDEX, queryEmbedding, request, 0, HYBRID_CANDIDATE_SIZE);
    logger.info("Hybrid lexical movie search query json: [{}]", lexicalRequest);
    logger.info("Hybrid semantic movie search query json: [{}]", semanticRequest);

    try {
      List<MovieSearchDocument> lexicalResults = searchDocuments(lexicalRequest);
      List<MovieSearchDocument> semanticResults = searchDocuments(semanticRequest);
      List<MovieSearchDocument> fusedCandidates =
          movieSearchRankFusion.fuse(lexicalResults, semanticResults, 0, HYBRID_CANDIDATE_SIZE);
      return toPagedCandidateMovieResponse(fusedCandidates, page, size);
    } catch (IOException ex) {
      throw new ElasticsearchOperationException("error while hybrid search was performed", ex);
    }
  }

  @Override
  public PagedResponse<MovieRecord> searchMoviesSemantically(
      String query, MovieSearchRequest request, int page, int size) {
    String normalizedQuery = query == null ? "" : query.trim();
    if (normalizedQuery.isBlank()) {
      return searchMovies(query, request, page, size);
    }

    float[] queryEmbedding = movieEmbeddingClient.embedText(normalizedQuery);
    SearchRequest searchRequest =
        movieSearchQueryBuilder.buildSemanticSearchRequest(
            MOVIES_INDEX, queryEmbedding, request, page, size);
    logger.info("Semantic movie search query json: [{}]", searchRequest);

    try {
      SearchResponse<MovieSearchDocument> response =
          esClient.search(searchRequest, MovieSearchDocument.class);
      logger.info(
          "Scores of semantically found documents: [{}]",
          response.hits().hits().stream().map(Hit::score).toList());
      return toPagedMovieResponse(response, page, size);
    } catch (IOException ex) {
      throw new ElasticsearchOperationException("error while semantic search was performed", ex);
    }
  }

  private PagedResponse<MovieRecord> toPagedMovieResponse(
      SearchResponse<MovieSearchDocument> response, int page, int size) {
    int totalHits = (int) (response.hits().total() != null ? response.hits().total().value() : 0);
    List<MovieSearchDocument> movies =
        response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();

    return toPagedMovieResponse(movies, page, size, totalHits);
  }

  private PagedResponse<MovieRecord> toPagedCandidateMovieResponse(
      List<MovieSearchDocument> movies, int page, int size) {
    int from = Math.min(page * size, movies.size());
    int to = Math.min(from + size, movies.size());
    List<MovieSearchDocument> pageContent = movies.subList(from, to);
    return toPagedMovieResponse(pageContent, page, size, movies.size());
  }

  private PagedResponse<MovieRecord> toPagedMovieResponse(
      List<MovieSearchDocument> pageContent, int page, int size, int totalHits) {
    Pageable pageable = PageRequest.of(page, size);

    return PagedResponse.from(
        new PageImpl<>(pageContent, pageable, totalHits).map(movieSearchDocumentMapper::toMovieRecord));
  }

  private List<MovieSearchDocument> searchDocuments(SearchRequest request) throws IOException {
    SearchResponse<MovieSearchDocument> response = esClient.search(request, MovieSearchDocument.class);
    return response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();
  }

  @Override
  public BoolQuery buildBoolQuery(String query, MovieSearchRequest request) {
    return movieSearchQueryBuilder.buildBoolQuery(query, request);
  }
}
// spotless:on
