package com.thecodinglab.imdbclone.service.impl;

import static com.thecodinglab.imdbclone.utility.Log.MOVIE_ID;
import static net.logstash.logback.argument.StructuredArguments.kv;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.payload.PagedResponse;
import com.thecodinglab.imdbclone.payload.movie.MovieSearchRequest;
import com.thecodinglab.imdbclone.service.ElasticSearchService;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// spotless:off
@Service
public class ElasticSearchServiceImpl implements ElasticSearchService {

  private static final Logger logger = LoggerFactory.getLogger(ElasticSearchServiceImpl.class);
  private final ElasticsearchClient esClient;
  private static final String MOVIES_INDEX = "movies";

  public ElasticSearchServiceImpl(ElasticsearchClient esClient) {
    this.esClient = esClient;
  }

  @Override
  public void indexMovie(Movie movie) {
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

    } catch (IOException e) {
      logger.error(
          "Document of type movie with [{}] was not indexed successfully.", kv(MOVIE_ID,movie.getId()));
      throw new RuntimeException(e);
    }
  }

  @Override
  public void indexMovies(List<Movie> movies) {
    BulkRequest.Builder br = new BulkRequest.Builder();
    for (Movie movie : movies) {
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
    } catch (IOException e) {
      throw new RuntimeException(e);
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
  public Movie getMovieDocumentById(Long movieId) {
    try {
      GetResponse<Movie> response = esClient
          .get(g -> g
                  .index(MOVIES_INDEX)
                  .id(movieId.toString()),
              Movie.class);

      logger.info("Movie document with primaryTitle [{}] was found.", response.source() != null ? response.source().getPrimaryTitle() : null);
      return response.source();
    } catch (IOException e) {
      logger.error("Movie document with [{}] was not found", kv(MOVIE_ID, movieId));
      throw new RuntimeException(e);
    }
  }

  /**
   * Search movies by Primary Title
   */
  public List<Movie> searchMoviesByPrimaryTitle(String searchText) {
    SearchResponse<Movie> response;
    try {
      response = esClient
          .search(s -> s
                  .index(MOVIES_INDEX)
                  .query(q -> q
                      .match(m -> m
                          .field("primaryTitle")
                          .query(searchText))),
              Movie.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    List<Movie> movies = response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();
    logger.info("Document search by primaryTitle gave [{}] results.", movies.size());
    return movies;
  }

  /**
   * Search movies by range of ratings
   */
  public List<Movie> searchMoviesByRatingRange(float minRating, float maxRating) {
    try {
      SearchResponse<Movie> response = esClient
          .search(s -> s
                  .index(MOVIES_INDEX)
                  .query(q -> q
                      .range(r -> r
                          .field("imdbRating")
                          .gte(JsonData.of(minRating))
                          .lte(JsonData.of(maxRating)))),
              Movie.class);
      List<Movie> movies = response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();
      logger.info("Document search by ratings between [{}] and [{}] gave [{}] results.",minRating, maxRating, movies.size());
      return movies;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Search Movies by Multiple Parameters, highest voted movies scoring is boosted
   */
  @Override
  public PagedResponse<Movie> searchMovies(String query, MovieSearchRequest request, int page, int size) {
    BoolQuery boolQuery = buildBoolQuery(query, request);
    SearchResponse<Movie> response;

    SearchRequest searchRequest =
        SearchRequest.of(s -> s
            .index(MOVIES_INDEX)
            .query(q -> q.bool(boolQuery))
            .from(page * size)
            .size(size)
        );
    logger.info("Document search query json: [{}]", searchRequest);

    try {
      response = esClient.search(searchRequest, Movie.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    List<Movie> movies =
        response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();

    logger.info(
        "Scores of found documents: [{}]",
        response.hits().hits().stream().map(Hit::score).toList());

    int totalHits = (int) (response.hits().total() != null ? response.hits().total().value() : 0);
    int totalPages = (int) Math.ceil((double) totalHits / size);
    boolean isLast = (page + 1) == totalPages;

    return new PagedResponse<>(movies, page, size, totalHits, totalPages, isLast);
  }

  @Override
  public BoolQuery buildBoolQuery(String query, MovieSearchRequest request) {
    BoolQuery.Builder search = QueryBuilders.bool();

    // -- highest voted movies scoring is boosted
    search.must(QueryBuilders
        .functionScore(fs -> fs
            .query(QueryBuilders
                .multiMatch(m -> m
                    .query(query)
                    .type(TextQueryType.MostFields)
                    .fields(List.of("primaryTitle", "originalTitle"))))
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
    if (request.minStartYear() != null && request.maxStartYear() != null) {
      search.filter(
          QueryBuilders.range(r -> r
              .field("startYear")
              .gte(JsonData.of(request.minStartYear()))
              .lte(JsonData.of(request.maxStartYear()))));
    }
    if (request.minRuntimeMinutes() != null && request.maxRuntimeMinutes() != null) {
      search.filter(
          QueryBuilders.range(r -> r
              .field("runtimeMinutes")
              .gte(JsonData.of(request.minRuntimeMinutes()))
              .lte(JsonData.of(request.maxRuntimeMinutes()))));
    }
    return search.build();
  }
}
// spotless:on
