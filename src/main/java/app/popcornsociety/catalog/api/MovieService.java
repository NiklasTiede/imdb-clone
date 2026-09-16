package app.popcornsociety.catalog.api;

import app.popcornsociety.shared.api.MessageResponse;
import app.popcornsociety.shared.api.PagedResponse;

public interface MovieService
    extends MovieReferenceService, MovieRatingAggregateService, MovieImageService {

  MovieRecord createMovie(MovieRequest movieRequest);

  MovieRecord updateMovie(Long movieId, MovieRequest request);

  MessageResponse deleteMovie(Long movieId);

  PagedResponse<MovieRecord> searchMoviesByTitle(String title, int page, int size);
}
