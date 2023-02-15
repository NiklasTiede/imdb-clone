package com.thecodinglab.imdbclone.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.payload.PagedResponse;
import com.thecodinglab.imdbclone.payload.movie.MovieSearchRequest;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface ElasticSearchService {

  void indexMovie(Movie movie);

  void indexMovies(List<Movie> movies);

  Movie getMovieDocumentById(Long movieId);

  List<Movie> searchMoviesByPrimaryTitle(String searchText);

  List<Movie> searchMoviesByRatingRange(float minRating, float maxRating);

  PagedResponse<Movie> searchMovies(MovieSearchRequest request, int page, int size);

  BoolQuery buildBoolQuery(MovieSearchRequest request);
}
