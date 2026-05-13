package com.thecodinglab.imdbclone.catalog.api;

import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("reference")
public interface MovieReferenceService {

  MovieRecord findMovieById(Long movieId);

  PagedResponse<MovieRecord> findMoviesByIds(List<Long> movieIds, int page, int size);
}
