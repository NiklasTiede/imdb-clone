package com.thecodinglab.imdbclone.catalog.internal.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.payload.PagedResponse;
import java.util.List;

public interface MovieSearchService {

  void indexMovie(Movie movie);

  void indexMovies(List<Movie> movies);

  Movie getMovieDocumentById(Long movieId);

  List<Movie> searchMoviesByPrimaryTitle(String searchText);

  List<Movie> searchMoviesByRatingRange(float minRating, float maxRating);

  PagedResponse<MovieRecord> searchMovies(
      String query, MovieSearchRequest request, int page, int size);

  BoolQuery buildBoolQuery(String query, MovieSearchRequest request);
}
