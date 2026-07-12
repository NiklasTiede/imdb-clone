package com.thecodinglab.imdbclone.recommendation.internal.home;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCandidateProvider;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCriteria;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryTheme;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryThemeEmbedding;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryThemeEmbeddingProvider;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeSectionComposerTest {

  @Mock private MovieDiscoveryCandidateProvider candidateProvider;
  @Mock private MovieDiscoveryThemeEmbeddingProvider themeEmbeddingProvider;

  @Test
  void compose_ranksCandidatesAndAddsThemToTheFeedWideSeenSet() {
    when(candidateProvider.findCandidates(any(), any(Integer.class))).thenReturn(movies(1, 12));
    HomeSectionComposer composer =
        new HomeSectionComposer(candidateProvider, themeEmbeddingProvider);
    Set<Long> seenMovieIds = Set.of(1L);
    Set<Long> mutableSeenMovieIds = new java.util.HashSet<>(seenMovieIds);

    var section = composer.compose(definition(), "seed", mutableSeenMovieIds);

    assertThat(section).isNotNull();
    assertThat(section.items())
        .hasSize(11)
        .extracting(item -> item.movie().id())
        .doesNotContain(1L);
    assertThat(mutableSeenMovieIds)
        .containsAll(section.items().stream().map(item -> item.movie().id()).toList());
  }

  @Test
  void compose_skipsSectionsThatCannotFillEightMovieSlots() {
    when(candidateProvider.findCandidates(any(), any(Integer.class))).thenReturn(movies(1, 7));

    assertThat(
            new HomeSectionComposer(candidateProvider, themeEmbeddingProvider)
                .compose(definition(), "seed", new java.util.HashSet<>()))
        .isNull();
  }

  @Test
  void compose_usesThePrecomputedThemeEmbeddingForSemanticSections() {
    MovieDiscoveryTheme theme = new MovieDiscoveryTheme("found-family", "Chosen family", 1);
    when(themeEmbeddingProvider.findEmbedding(theme))
        .thenReturn(
            java.util.Optional.of(
                new MovieDiscoveryThemeEmbedding("found-family", 1, "local-model", List.of(0.1f))));
    when(candidateProvider.findSemanticCandidates(any(), any(), eq(90))).thenReturn(movies(1, 12));

    var section =
        new HomeSectionComposer(candidateProvider, themeEmbeddingProvider)
            .compose(semanticDefinition(theme), "seed", new java.util.HashSet<>());

    assertThat(section).isNotNull();
    verify(candidateProvider).findSemanticCandidates(any(), any(), eq(90));
  }

  @Test
  void featured_selectsThreeMoviesAndReservesThemFromLaterSections() {
    when(candidateProvider.findCandidates(any(), eq(90))).thenReturn(movies(1, 12));
    Set<Long> seenMovieIds = new java.util.HashSet<>();

    List<MovieRecord> featured =
        new HomeSectionComposer(candidateProvider, themeEmbeddingProvider)
            .featured(new HomeSectionCatalog(), "seed", seenMovieIds);

    assertThat(featured).hasSize(3);
    assertThat(seenMovieIds)
        .containsExactlyInAnyOrderElementsOf(featured.stream().map(MovieRecord::id).toList());
  }

  private HomeSectionDefinition definition() {
    return new HomeSectionDefinition(
        "section",
        "A section",
        "A clear reason",
        HomeSectionFamily.GENRE,
        null,
        new MovieDiscoveryCriteria(
            null, null, null, null, Set.of(), MovieType.MOVIE, null, null, Set.of()),
        90,
        12);
  }

  private HomeSectionDefinition semanticDefinition(MovieDiscoveryTheme theme) {
    return new HomeSectionDefinition(
        "found-family",
        "Found family",
        "Unexpected people becoming home",
        HomeSectionFamily.THEME,
        theme,
        new MovieDiscoveryCriteria(
            null, null, null, null, Set.of(), MovieType.MOVIE, null, null, Set.of()),
        90,
        12);
  }

  private List<MovieRecord> movies(long firstId, long lastId) {
    return LongStream.rangeClosed(firstId, lastId)
        .mapToObj(
            id ->
                new MovieRecord(
                    id,
                    null,
                    null,
                    MovieType.MOVIE,
                    "Movie " + id,
                    "Movie " + id,
                    false,
                    2020,
                    null,
                    100,
                    null,
                    null,
                    Set.of(),
                    7.0f + id / 100.0f,
                    1_000,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null))
        .toList();
  }
}
