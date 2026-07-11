package com.thecodinglab.imdbclone.catalog.internal.search.embedding;

import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryTheme;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryThemeEmbedding;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryThemeEmbeddingProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InMemoryMovieDiscoveryThemeEmbeddings implements MovieDiscoveryThemeEmbeddingProvider {

  private static final Logger logger =
      LoggerFactory.getLogger(InMemoryMovieDiscoveryThemeEmbeddings.class);

  private final MovieEmbeddingClient movieEmbeddingClient;
  private final Map<String, MovieDiscoveryThemeEmbedding> embeddings = new ConcurrentHashMap<>();

  public InMemoryMovieDiscoveryThemeEmbeddings(MovieEmbeddingClient movieEmbeddingClient) {
    this.movieEmbeddingClient = movieEmbeddingClient;
  }

  @Override
  public Optional<MovieDiscoveryThemeEmbedding> findEmbedding(MovieDiscoveryTheme theme) {
    MovieDiscoveryThemeEmbedding embedding = embeddings.get(theme.id());
    if (embedding == null
        || embedding.promptVersion() != theme.promptVersion()
        || !embedding.modelName().equals(movieEmbeddingClient.modelName())) {
      return Optional.empty();
    }
    return Optional.of(embedding);
  }

  @Override
  public void reconcile(Collection<MovieDiscoveryTheme> themes) {
    for (MovieDiscoveryTheme theme : themes) {
      reconcile(theme);
    }
  }

  private void reconcile(MovieDiscoveryTheme theme) {
    MovieDiscoveryThemeEmbedding current = embeddings.get(theme.id());
    if (current != null
        && current.promptVersion() == theme.promptVersion()
        && current.modelName().equals(movieEmbeddingClient.modelName())) {
      return;
    }

    try {
      float[] embedding = movieEmbeddingClient.embedText(theme.prompt());
      if (embedding == null || embedding.length == 0) {
        logger.warn("Movie discovery theme embedding was empty themeId={}", theme.id());
        return;
      }
      embeddings.put(
          theme.id(),
          new MovieDiscoveryThemeEmbedding(
              theme.id(),
              theme.promptVersion(),
              movieEmbeddingClient.modelName(),
              toFloatList(embedding)));
      logger.info("Reconciled movie discovery theme embedding themeId={}", theme.id());
    } catch (RuntimeException ex) {
      logger.warn("Could not reconcile movie discovery theme embedding themeId={}", theme.id(), ex);
    }
  }

  private List<Float> toFloatList(float[] values) {
    List<Float> floats = new ArrayList<>(values.length);
    for (float value : values) {
      floats.add(value);
    }
    return floats;
  }
}
