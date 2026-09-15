package com.thecodinglab.imdbclone.engagement.internal;

import com.thecodinglab.imdbclone.engagement.api.*;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import org.springframework.stereotype.Service;

@Service
class AssistantRatingReads implements AssistantRatingLibrary {
  private final AccountLibraryService library;

  AssistantRatingReads(AccountLibraryService library) {
    this.library = library;
  }

  @Override
  public Result read(Long accountId, int page, Order order) {
    if (page < 0 || page > 100 || order == null)
      throw new IllegalArgumentException("Invalid ratings page or order");
    var result =
        library.getRatingLibrary(
            accountId,
            page,
            20,
            switch (order) {
              case HIGHEST -> RatingLibrarySort.SCORE_DESC;
              case LOWEST -> RatingLibrarySort.SCORE_ASC;
              case RECENT -> RatingLibrarySort.RATED_AT_DESC;
            });
    var items = result.items();
    return new Result(
        new PagedResponse<>(
            items.getContent().stream()
                .map(r -> new Entry(r.movie(), r.rating(), r.ratedAt()))
                .toList(),
            items.getPage(),
            items.getSize(),
            items.getTotalElements(),
            items.getTotalPages(),
            items.isLast()),
        result.insights().averageUserRating(),
        result.insights().favoriteGenres().stream()
            .map(f -> new Facet(f.label(), f.movieCount(), f.averageUserRating()))
            .toList(),
        result.insights().favoriteDecades().stream()
            .map(f -> new Facet(f.label(), f.movieCount(), f.averageUserRating()))
            .toList());
  }
}
