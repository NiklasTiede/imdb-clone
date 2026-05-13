package com.thecodinglab.imdbclone.catalog.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface({"reference", "ratings"})
public enum MovieType {
  SHORT,
  MOVIE,
  VIDEO,
  TV_MOVIE,
  TV_EPISODE,
  TV_MINI_SERIES,
  TV_SPECIAL,
  TV_SERIES,
  TV_SHORT,
  TV_PILOT,
  VIDEO_GAME
}
