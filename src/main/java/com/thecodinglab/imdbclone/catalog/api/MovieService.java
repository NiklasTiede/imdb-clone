package com.thecodinglab.imdbclone.catalog.api;

import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import java.math.BigDecimal;
import java.util.List;

public interface MovieService {

  MovieRecord findMovieById(Long movieId);

  PagedResponse<MovieRecord> findMoviesByIds(List<Long> movieIds, int page, int size);

  MovieRecord createMovie(MovieRequest movieRequest);

  MovieRecord updateMovie(Long movieId, MovieRequest request);

  MessageResponse deleteMovie(Long movieId);

  PagedResponse<MovieRecord> searchMoviesByTitle(String title, int page, int size);

  MovieRecord updateRatingAggregate(Long movieId, BigDecimal rating, int ratingCount);

  MovieImageToken getMovieImageToken(Long movieId);

  MovieImageToken updateMovieImageToken(Long movieId, String imageUrlToken);
}
