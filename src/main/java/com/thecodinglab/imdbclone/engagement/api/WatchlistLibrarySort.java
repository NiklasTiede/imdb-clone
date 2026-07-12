package com.thecodinglab.imdbclone.engagement.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public enum WatchlistLibrarySort {
  ADDED_AT_DESC,
  ADDED_AT_ASC,
  IMDB_DESC,
  IMDB_ASC,
  RUNTIME_DESC,
  RUNTIME_ASC,
  TITLE_ASC
}
