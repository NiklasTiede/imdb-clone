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
  private static final String MOVIES_INDEX = "movies";

  public ElasticsearchMovieSearchService(
      ElasticsearchClient esClient,
      MovieSearchDocumentMapper movieSearchDocumentMapper,
      MovieEmbeddingClient movieEmbeddingClient,
      MovieSearchQueryBuilder movieSearchQueryBuilder) {
    this.esClient = esClient;
    this.movieSearchDocumentMapper = movieSearchDocumentMapper;
    this.movieEmbeddingClient = movieEmbeddingClient;
    this.movieSearchQueryBuilder = movieSearchQueryBuilder;
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
    Pageable pageable = PageRequest.of(page, size);

    List<MovieSearchDocument> movies =
        response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();

    return PagedResponse.from(
        new PageImpl<>(movies, pageable, totalHits).map(movieSearchDocumentMapper::toMovieRecord));
  }

  @Override
  public BoolQuery buildBoolQuery(String query, MovieSearchRequest request) {
    BoolQuery.Builder search = QueryBuilders.bool();
    String normalizedQuery = query == null ? "" : query.trim();
    Query textQuery =
        normalizedQuery.isBlank()
            ? QueryBuilders.matchAll().build()._toQuery()
            : QueryBuilders
                .multiMatch(m -> m
                    .query(normalizedQuery)
                    .type(TextQueryType.MostFields)
                    .fields(List.of("primaryTitle", "originalTitle")));

    // -- highest voted movies scoring is boosted
    search.must(QueryBuilders
        .functionScore(fs -> fs
            .query(textQuery)
            .functions(FunctionScore
                .of(f -> f
                    .fieldValueFactor(FunctionScoreBuilders
                        .fieldValueFactor()
                        .factor(0.002)
                        .field("imdbRatingCount")
                        .modifier(FieldValueFactorModifier.Log1p).build())))
            .scoreMode(FunctionScoreMode.Multiply)));

    // -- results are filtrated by these parameters
    if (request.movieGenre() != null && !request.movieGenre().isEmpty()) {
      request.movieGenre().forEach(
          movieGenreEnum -> search.filter(QueryBuilders
              .match(m -> m
                  .field("movieGenre")
                  .query(String.valueOf(movieGenreEnum))
              )));
    }
    if (request.movieType() != null) {
      search.filter(
          QueryBuilders.match(m -> m
              .field("movieType")
              .query(request.movieType().toString())));
    }
    if (request.minStartYear() != null || request.maxStartYear() != null) {
      search.filter(
          QueryBuilders.range(r -> r
              .number(n -> n
                  .field("startYear")
                  .gte(request.minStartYear() == null ? null : request.minStartYear().doubleValue())
                  .lte(request.maxStartYear() == null ? null : request.maxStartYear().doubleValue()))));
    }
    if (request.minRuntimeMinutes() != null || request.maxRuntimeMinutes() != null) {
      search.filter(
          QueryBuilders.range(r -> r
              .number(n -> n
                  .field("runtimeMinutes")
                  .gte(request.minRuntimeMinutes() == null ? null : request.minRuntimeMinutes().doubleValue())
                  .lte(request.maxRuntimeMinutes() == null ? null : request.maxRuntimeMinutes().doubleValue()))));
    }
    return search.build();
  }
}
// spotless:on
