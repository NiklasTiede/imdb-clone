package com.thecodinglab.imdbclone.catalog.internal.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import java.util.List;

public interface MovieSearchService {

  void indexMovie(MovieSearchDocument movie);

  void indexMovies(List<MovieSearchDocument> movies);

  MovieSearchDocument getMovieDocumentById(Long movieId);

  List<MovieSearchDocument> searchMoviesByPrimaryTitle(String searchText);

  List<MovieSearchDocument> searchMoviesByRatingRange(float minRating, float maxRating);

  PagedResponse<MovieRecord> searchMovies(
      String query, MovieSearchRequest request, int page, int size);

  PagedResponse<MovieRecord> searchMoviesSemantically(
      String query, MovieSearchRequest request, int page, int size);

  BoolQuery buildBoolQuery(String query, MovieSearchRequest request);
}
