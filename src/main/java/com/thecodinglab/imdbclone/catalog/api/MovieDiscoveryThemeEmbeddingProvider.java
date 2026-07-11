package com.thecodinglab.imdbclone.catalog.api;

import java.util.Collection;
import java.util.Optional;
import org.springframework.modulith.NamedInterface;

@NamedInterface("recommendation")
public interface MovieDiscoveryThemeEmbeddingProvider {

  Optional<MovieDiscoveryThemeEmbedding> findEmbedding(MovieDiscoveryTheme theme);

  void reconcile(Collection<MovieDiscoveryTheme> themes);
}
