package com.thecodinglab.imdbclone.engagement.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public interface AccountLibraryService {

  RatingLibraryResponse getRatingLibrary(
      Long accountId, int page, int size, RatingLibrarySort sort);

  WatchlistLibraryResponse getWatchlistLibrary(
      Long accountId, int page, int size, WatchlistLibrarySort sort);
}
