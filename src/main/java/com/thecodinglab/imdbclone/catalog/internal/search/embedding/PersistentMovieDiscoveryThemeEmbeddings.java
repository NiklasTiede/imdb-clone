package com.thecodinglab.imdbclone.catalog.internal.search.embedding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryTheme;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryThemeEmbedding;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryThemeEmbeddingProvider;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieDiscoveryThemeEmbeddingEntity;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieDiscoveryThemeEmbeddingRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersistentMovieDiscoveryThemeEmbeddings
    implements MovieDiscoveryThemeEmbeddingProvider {
  private static final Logger logger =
      LoggerFactory.getLogger(PersistentMovieDiscoveryThemeEmbeddings.class);
  private static final TypeReference<List<Float>> FLOAT_LIST = new TypeReference<>() {};
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final MovieEmbeddingClient movieEmbeddingClient;
  private final MovieDiscoveryThemeEmbeddingRepository repository;

  public PersistentMovieDiscoveryThemeEmbeddings(
      MovieEmbeddingClient movieEmbeddingClient,
      MovieDiscoveryThemeEmbeddingRepository repository) {
    this.movieEmbeddingClient = movieEmbeddingClient;
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<MovieDiscoveryThemeEmbedding> findEmbedding(MovieDiscoveryTheme theme) {
    return repository
        .findById(theme.id())
        .filter(entity -> entity.getPromptVersion() == theme.promptVersion())
        .filter(entity -> entity.getModelName().equals(movieEmbeddingClient.modelName()))
        .flatMap(this::toEmbedding);
  }

  @Override
  @Transactional
  public void reconcile(Collection<MovieDiscoveryTheme> themes) {
    for (MovieDiscoveryTheme theme : themes) reconcile(theme);
  }

  private void reconcile(MovieDiscoveryTheme theme) {
    if (findEmbedding(theme).isPresent()) return;
    try {
      float[] vector = movieEmbeddingClient.embedText(theme.prompt());
      if (vector == null || vector.length == 0) {
        logger.warn("Movie discovery theme embedding was empty themeId={}", theme.id());
        return;
      }
      List<Float> embedding = toFloatList(vector);
      repository.save(
          new MovieDiscoveryThemeEmbeddingEntity(
              theme.id(),
              theme.promptVersion(),
              movieEmbeddingClient.modelName(),
              embedding.size(),
              objectMapper.writeValueAsString(embedding)));
      logger.info(
          "Persisted movie discovery theme embedding themeId={} promptVersion={} model={}",
          theme.id(),
          theme.promptVersion(),
          movieEmbeddingClient.modelName());
    } catch (JsonProcessingException | RuntimeException ex) {
      logger.warn("Could not reconcile movie discovery theme embedding themeId={}", theme.id(), ex);
    }
  }

  private Optional<MovieDiscoveryThemeEmbedding> toEmbedding(
      MovieDiscoveryThemeEmbeddingEntity entity) {
    try {
      List<Float> embedding = objectMapper.readValue(entity.getEmbeddingJson(), FLOAT_LIST);
      return embedding.size() == entity.getDimensions() && !embedding.isEmpty()
          ? Optional.of(
              new MovieDiscoveryThemeEmbedding(
                  entity.getThemeId(), entity.getPromptVersion(), entity.getModelName(), embedding))
          : Optional.empty();
    } catch (JsonProcessingException ex) {
      logger.warn(
          "Could not read persisted movie discovery theme embedding themeId={}",
          entity.getThemeId(),
          ex);
      return Optional.empty();
    }
  }

  private List<Float> toFloatList(float[] values) {
    List<Float> floats = new ArrayList<>(values.length);
    for (float value : values) floats.add(value);
    return floats;
  }
}
