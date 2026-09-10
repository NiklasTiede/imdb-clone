package com.thecodinglab.imdbclone.catalog.internal.enrichment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("imdb-clone.catalog.tmdb")
public record TmdbProperties(String readAccessToken) {
  public TmdbProperties {
    readAccessToken = readAccessToken == null ? "" : readAccessToken.trim();
  }

  public boolean enabled() {
    return !readAccessToken.isEmpty();
  }

  @Override
  public String toString() {
    return "TmdbProperties[readAccessToken=REDACTED]";
  }
}
