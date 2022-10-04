package com.thecodinglab.imdbclone.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.thecodinglab.imdbclone.entity.Movie;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// spotless:off
@Component
public class EsUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(EsUtils.class);

  private final ElasticsearchClient esClient;

  public EsUtils(EsClientConfig esClientConfig) {
    this.esClient = esClientConfig.getClient();
  }

  /**
   * Single Index
   */
  public void indexMovie(Movie movie) {
    IndexResponse indexResponse;
    try {
      indexResponse =
              esClient.index(
                      builder -> builder
                              .index("movies")
                              .id(movie.getId().toString())
                              .document(movie));
      LOGGER.info("Document of type Movie with id [{}] of index [{}] was [{}].",
              indexResponse.id(),
              indexResponse.index(),
              indexResponse.result().jsonValue()
      );
    } catch (IOException e) {
      LOGGER.error("Document of type movie with id [{}] was not indexed successfully.", movie.getId());
      throw new RuntimeException(e);
    }
  }

  /**
   * Bulk Index
   */
  public void indexMovies(List<Movie> movies) {
    BulkRequest.Builder br = new BulkRequest.Builder();
    for (Movie movie : movies) {
      br.operations(op -> op
              .index(idx -> idx
                      .index("movies")
                      .id(movie.getId().toString())
                      .document(movie)));
    }
    BulkResponse result;
    try {
      result = esClient.bulk(br.build());
      LOGGER.info("Number of documents which were [{}] is [{}]",
              result.items().stream().map(BulkResponseItem::result).filter(Objects::nonNull).collect(Collectors.toSet()),
              result.items().size());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (result.errors()) {
      LOGGER.error("Bulk had errors");
      for (BulkResponseItem item : result.items()) {
        if (item.error() != null) {
          LOGGER.error(item.error().reason());
        }
      }
    }
  }

  /**
   * Search Single Document by ID
   */
  public Movie getMovieDocumentById(Long movieId) {
    try {
      GetResponse<Movie> response = esClient.get(g -> g.index("movies").id(movieId.toString()), Movie.class);
      LOGGER.info("Movie document with primaryTitle [{}] was found.", response.source() != null ? response.source().getPrimaryTitle() : null);
      return response.source();
    } catch (IOException e) {
      LOGGER.error("Movie document with id [{}] was not found", movieId);
      throw new RuntimeException(e);
    }
  }

  /**
   * Search movies by Primary Title
   */
  public List<Movie> searchMoviesByPrimaryTitle(String searchText) {
    SearchResponse<Movie> response = null;
    try {
      response = esClient.search(s -> s.index("movies").query(q -> q.match(t -> t.field("primaryTitle").query(searchText))), Movie.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    List<Movie> movies = response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();
    LOGGER.info("Document search by primaryTitle gave [{}] results.", movies.size());
    return movies;
  }

  /**
   * Search movies by range of ratings
   */
  public List<Movie> searchMoviesByRatingRange(float minRating, float maxRating) {
    try {
      SearchResponse<Movie> response = esClient.search(s -> s.index("movies").query(q -> q.range(t -> t.field("imdbRating").gte(JsonData.of(minRating)).lte(JsonData.of(maxRating)))), Movie.class);
      System.out.println(response.hits().hits().size());
      List<Movie> movies = response.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).toList();
      LOGGER.info("Document search by ratings between [{}] and [{}] gave [{}] results.",minRating, maxRating, movies.size());
      return movies;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
// spotless:on
