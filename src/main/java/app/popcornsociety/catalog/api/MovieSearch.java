package app.popcornsociety.catalog.api;

import app.popcornsociety.shared.api.PagedResponse;
import org.springframework.modulith.NamedInterface;

/** Public catalog use case for finding movies. */
@NamedInterface("assistant")
public interface MovieSearch {

  PagedResponse<MovieRecord> searchMovies(
      String query, MovieSearchRequest request, int page, int size);
}
