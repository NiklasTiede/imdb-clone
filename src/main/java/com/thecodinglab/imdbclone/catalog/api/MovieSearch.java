package com.thecodinglab.imdbclone.catalog.api;

import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import org.springframework.modulith.NamedInterface;

/** Public catalog use case for finding movies. */
@NamedInterface("assistant")
public interface MovieSearch {

  PagedResponse<MovieRecord> searchMovies(
      String query, MovieSearchRequest request, int page, int size);
}
