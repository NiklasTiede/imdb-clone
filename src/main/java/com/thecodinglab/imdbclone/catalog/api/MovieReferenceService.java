package com.thecodinglab.imdbclone.catalog.api;

import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import java.util.Collection;
import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("reference")
public interface MovieReferenceService {

  MovieRecord findMovieById(Long movieId);

  List<MovieRecord> findMoviesByIds(Collection<Long> movieIds);

  PagedResponse<MovieRecord> findMoviesByIds(List<Long> movieIds, int page, int size);
}
