package com.thecodinglab.imdbclone.engagement.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public enum RatingLibrarySort {
  SCORE_DESC,
  SCORE_ASC,
  RATED_AT_DESC,
  RATED_AT_ASC,
  IMDB_DESC,
  IMDB_ASC,
  TITLE_ASC
}
