package com.thecodinglab.imdbclone.catalog.api;

import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;

public interface MovieService
    extends MovieReferenceService, MovieRatingAggregateService, MovieImageService {

  MovieRecord createMovie(MovieRequest movieRequest);

  MovieRecord updateMovie(Long movieId, MovieRequest request);

  MessageResponse deleteMovie(Long movieId);

  PagedResponse<MovieRecord> searchMoviesByTitle(String title, int page, int size);
}
