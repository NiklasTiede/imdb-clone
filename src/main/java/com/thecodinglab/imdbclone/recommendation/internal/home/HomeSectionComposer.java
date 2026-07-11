package com.thecodinglab.imdbclone.recommendation.internal.home;

import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCandidateProvider;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryThemeEmbeddingProvider;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.recommendation.api.HomeFeedItem;
import com.thecodinglab.imdbclone.recommendation.api.HomeFeedSection;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
class HomeSectionComposer {

  private static final int MINIMUM_SECTION_SIZE = 8;

  private final MovieDiscoveryCandidateProvider candidateProvider;
  private final MovieDiscoveryThemeEmbeddingProvider themeEmbeddingProvider;
  private final HomeDiscoveryRanker ranker = new HomeDiscoveryRanker();

  HomeSectionComposer(
      MovieDiscoveryCandidateProvider candidateProvider,
      MovieDiscoveryThemeEmbeddingProvider themeEmbeddingProvider) {
    this.candidateProvider = candidateProvider;
    this.themeEmbeddingProvider = themeEmbeddingProvider;
  }

  HomeFeedSection compose(
      HomeSectionDefinition definition, String feedSeed, Set<Long> seenMovieIds) {
    List<MovieRecord> candidates = findCandidates(definition, seenMovieIds);
    List<MovieRecord> movies =
        ranker.rank(
            candidates,
            seenMovieIds,
            HomeFeedSeed.derive(feedSeed, definition.id()),
            definition.displayLimit());
    if (movies.size() < MINIMUM_SECTION_SIZE) {
      return null;
    }

    movies.stream().map(MovieRecord::id).forEach(seenMovieIds::add);
    return new HomeFeedSection(
        definition.id(),
        definition.title(),
        definition.subtitle(),
        definition.family().name(),
        movies.stream().map(movie -> new HomeFeedItem(movie, definition.subtitle())).toList());
  }

  MovieRecord featured(HomeSectionCatalog catalog, String feedSeed, Set<Long> seenMovieIds) {
    List<MovieRecord> candidates =
        candidateProvider.findCandidates(catalog.featuredCriteria().excluding(seenMovieIds), 90);
    List<MovieRecord> ranked =
        ranker.rank(candidates, seenMovieIds, HomeFeedSeed.derive(feedSeed, "featured"), 1);
    if (ranked.isEmpty()) {
      return null;
    }
    MovieRecord featured = ranked.getFirst();
    seenMovieIds.add(featured.id());
    return featured;
  }

  private List<MovieRecord> findCandidates(
      HomeSectionDefinition definition, Set<Long> seenMovieIds) {
    var criteria = definition.criteria().excluding(seenMovieIds);
    if (definition.semanticTheme() == null) {
      return candidateProvider.findCandidates(criteria, definition.candidateLimit());
    }
    return themeEmbeddingProvider
        .findEmbedding(definition.semanticTheme())
        .map(
            embedding ->
                candidateProvider.findSemanticCandidates(
                    criteria, embedding.toFloatArray(), definition.candidateLimit()))
        .orElseGet(List::of);
  }
}
