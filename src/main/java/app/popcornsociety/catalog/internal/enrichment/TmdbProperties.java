package app.popcornsociety.catalog.internal.enrichment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("popcorn-society.catalog.tmdb")
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
