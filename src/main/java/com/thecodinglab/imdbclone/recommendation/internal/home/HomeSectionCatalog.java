package com.thecodinglab.imdbclone.recommendation.internal.home;

import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCriteria;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryTheme;
import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
class HomeSectionCatalog {

  private static final int DEFAULT_CANDIDATE_LIMIT = 90;
  private static final int DEFAULT_DISPLAY_LIMIT = 12;

  private final List<HomeSectionDefinition> definitions =
      List.of(
          section(
              "acclaimed-movies",
              "Acclaimed movies",
              "Essential picks with staying power",
              HomeSectionFamily.BROAD_QUALITY,
              criteria(1980, null, null, null, Set.of(), MovieType.MOVIE, 7.5f, 1_000)),
          section(
              "crowd-favorites",
              "Crowd favorites",
              "Highly rated movies people return to",
              HomeSectionFamily.BROAD_QUALITY,
              criteria(1990, null, null, null, Set.of(), MovieType.MOVIE, 7.0f, 500)),
          section(
              "top-horror",
              "Top horror",
              "Chills worth staying up for",
              HomeSectionFamily.GENRE,
              criteria(
                  1980, null, null, null, Set.of(MovieGenre.HORROR), MovieType.MOVIE, 6.2f, 0)),
          section(
              "top-thrillers",
              "Top thrillers",
              "Tension from the first scene",
              HomeSectionFamily.GENRE,
              criteria(
                  1980, null, null, null, Set.of(MovieGenre.THRILLER), MovieType.MOVIE, 6.5f, 0)),
          section(
              "top-sci-fi",
              "Top sci-fi",
              "Worlds beyond our own",
              HomeSectionFamily.GENRE,
              criteria(
                  1980, null, null, null, Set.of(MovieGenre.SCI_FI), MovieType.MOVIE, 6.5f, 0)),
          section(
              "crime-stories",
              "Crime stories",
              "Cases, cons, and consequences",
              HomeSectionFamily.GENRE,
              criteria(1980, null, null, null, Set.of(MovieGenre.CRIME), MovieType.MOVIE, 6.5f, 0)),
          section(
              "animated-favorites",
              "Animated favorites",
              "Inventive worlds for every age",
              HomeSectionFamily.GENRE,
              criteria(
                  1980, null, null, null, Set.of(MovieGenre.ANIMATION), MovieType.MOVIE, 6.5f, 0)),
          section(
              "romantic-movies",
              "Romantic movies",
              "Stories that wear their heart openly",
              HomeSectionFamily.GENRE,
              criteria(
                  1980, null, null, null, Set.of(MovieGenre.ROMANCE), MovieType.MOVIE, 6.5f, 0)),
          section(
              "great-comedies",
              "Great comedies",
              "The kind you quote afterward",
              HomeSectionFamily.GENRE,
              criteria(
                  1980, null, null, null, Set.of(MovieGenre.COMEDY), MovieType.MOVIE, 6.5f, 0)),
          section(
              "adventure-awaits",
              "Adventure awaits",
              "Big journeys and bigger stakes",
              HomeSectionFamily.GENRE,
              criteria(
                  1980, null, null, null, Set.of(MovieGenre.ADVENTURE), MovieType.MOVIE, 6.2f, 0)),
          section(
              "great-dramas",
              "Great dramas",
              "Stories that stay with you",
              HomeSectionFamily.GENRE,
              criteria(1980, null, null, null, Set.of(MovieGenre.DRAMA), MovieType.MOVIE, 6.8f, 0)),
          section(
              "mystery-movies",
              "Mystery movies",
              "Follow every clue",
              HomeSectionFamily.GENRE,
              criteria(
                  1980, null, null, null, Set.of(MovieGenre.MYSTERY), MovieType.MOVIE, 6.4f, 0)),
          section(
              "nineties-thrillers",
              "1990s thrillers",
              "A decade built for suspense",
              HomeSectionFamily.ERA,
              criteria(
                  1990, 1999, null, null, Set.of(MovieGenre.THRILLER), MovieType.MOVIE, 6.2f, 0)),
          section(
              "eighties-favorites",
              "1980s favorites",
              "Big ideas, bold stories",
              HomeSectionFamily.ERA,
              criteria(1980, 1989, null, null, Set.of(), MovieType.MOVIE, 6.5f, 0)),
          section(
              "seventies-classics",
              "1970s classics",
              "A decade that rewrote the rules",
              HomeSectionFamily.ERA,
              criteria(1970, 1979, null, null, Set.of(), MovieType.MOVIE, 6.5f, 0)),
          section(
              "millennium-movies",
              "Turn-of-the-millennium movies",
              "The first decade of the 2000s",
              HomeSectionFamily.ERA,
              criteria(2000, 2009, null, null, Set.of(), MovieType.MOVIE, 6.5f, 0)),
          section(
              "modern-classics",
              "Modern classics",
              "Recent movies already earning their place",
              HomeSectionFamily.ERA,
              criteria(2010, 2019, null, null, Set.of(), MovieType.MOVIE, 7.0f, 0)),
          section(
              "under-100-minutes",
              "Under 100 minutes",
              "A complete movie night, no delay",
              HomeSectionFamily.RUNTIME,
              criteria(1980, null, 70, 100, Set.of(), MovieType.MOVIE, 6.5f, 0)),
          section(
              "weekend-epics",
              "Weekend epics",
              "For when you want to settle in",
              HomeSectionFamily.RUNTIME,
              criteria(1980, null, 150, 260, Set.of(), MovieType.MOVIE, 6.5f, 0)),
          section(
              "documentary-picks",
              "Documentary picks",
              "Remarkable stories from the real world",
              HomeSectionFamily.FORMAT,
              criteria(
                  1980,
                  null,
                  null,
                  null,
                  Set.of(MovieGenre.DOCUMENTARY),
                  MovieType.MOVIE,
                  6.5f,
                  0)),
          section(
              "miniseries",
              "Miniseries worth a weekend",
              "One complete story, beautifully contained",
              HomeSectionFamily.FORMAT,
              criteria(1980, null, null, null, Set.of(), MovieType.TV_MINI_SERIES, 6.5f, 0)),
          section(
              "tv-series",
              "TV series to get lost in",
              "Long-form stories with room to breathe",
              HomeSectionFamily.FORMAT,
              criteria(1980, null, null, null, Set.of(), MovieType.TV_SERIES, 6.5f, 0)),
          section(
              "tv-movie-gems",
              "TV movie gems",
              "Strong stories in a compact format",
              HomeSectionFamily.FORMAT,
              criteria(1980, null, null, null, Set.of(), MovieType.TV_MOVIE, 6.5f, 0)),
          section(
              "short-films",
              "Short films",
              "Small runtimes, lasting impressions",
              HomeSectionFamily.FORMAT,
              criteria(1980, null, 10, 60, Set.of(), MovieType.SHORT, 6.0f, 0)),
          semanticSection(
              "found-family",
              "Found family",
              "Unexpected people becoming home",
              theme("found-family", "Movies about found family, belonging, and chosen family")),
          semanticSection(
              "slow-burn-mysteries",
              "Slow-burn mysteries",
              "Patient stories with clues beneath the surface",
              theme(
                  "slow-burn-mysteries",
                  "Slow-burn mystery movies with atmospheric suspense and careful investigation")),
          semanticSection(
              "courtroom-tension",
              "Courtroom tension",
              "Arguments, evidence, and high stakes",
              theme(
                  "courtroom-tension",
                  "Courtroom dramas and legal thrillers with tense trials and moral dilemmas")),
          semanticSection(
              "space-exploration",
              "Space exploration",
              "Big questions at the edge of the unknown",
              theme(
                  "space-exploration",
                  "Movies about space exploration, astronauts, distant planets, and cosmic discovery")),
          semanticSection(
              "political-paranoia",
              "Political paranoia",
              "Power, secrets, and who to trust",
              theme(
                  "political-paranoia",
                  "Political thrillers about conspiracy, surveillance, corruption, and paranoia")),
          semanticSection(
              "quiet-grief",
              "Quiet films about grief",
              "Tender stories about loss and healing",
              theme(
                  "quiet-grief",
                  "Quiet reflective movies about grief, loss, healing, and human connection")),
          semanticSection(
              "heists-gone-wrong",
              "Heists gone wrong",
              "Plans unraveling in spectacular fashion",
              theme(
                  "heists-gone-wrong",
                  "Crime movies about heists gone wrong, betrayals, escapes, and consequences")),
          semanticSection(
              "small-town-secrets",
              "Small-town secrets",
              "Nothing stays hidden forever",
              theme(
                  "small-town-secrets",
                  "Mystery movies set in small towns with secrets, hidden histories, and close-knit communities")));

  List<HomeSectionDefinition> ordered(String seed) {
    List<HomeSectionDefinition> randomized = new ArrayList<>(definitions);
    randomized.sort(
        Comparator.comparingLong(definition -> HomeFeedSeed.derive(seed, definition.id())));

    List<HomeSectionDefinition> ordered = new ArrayList<>();
    addFirstFamily(randomized, ordered, HomeSectionFamily.BROAD_QUALITY);
    addFirstFamily(randomized, ordered, HomeSectionFamily.GENRE);
    addFirstFamily(randomized, ordered, HomeSectionFamily.ERA);

    HomeSectionFamily previous = ordered.isEmpty() ? null : ordered.getLast().family();
    while (!randomized.isEmpty()) {
      int index = firstDifferentFamilyIndex(randomized, previous);
      HomeSectionDefinition next = randomized.remove(index);
      ordered.add(next);
      previous = next.family();
    }
    return List.copyOf(ordered);
  }

  MovieDiscoveryCriteria featuredCriteria() {
    return criteria(1980, null, null, null, Set.of(), MovieType.MOVIE, 7.5f, 1_000);
  }

  List<MovieDiscoveryTheme> semanticThemes() {
    return definitions.stream()
        .map(HomeSectionDefinition::semanticTheme)
        .filter(theme -> theme != null)
        .distinct()
        .toList();
  }

  private void addFirstFamily(
      List<HomeSectionDefinition> source,
      List<HomeSectionDefinition> target,
      HomeSectionFamily family) {
    source.stream()
        .filter(definition -> definition.family() == family)
        .findFirst()
        .ifPresent(
            definition -> {
              source.remove(definition);
              target.add(definition);
            });
  }

  private int firstDifferentFamilyIndex(
      List<HomeSectionDefinition> source, HomeSectionFamily previousFamily) {
    for (int index = 0; index < source.size(); index++) {
      if (source.get(index).family() != previousFamily) {
        return index;
      }
    }
    return 0;
  }

  private HomeSectionDefinition section(
      String id,
      String title,
      String subtitle,
      HomeSectionFamily family,
      MovieDiscoveryCriteria criteria) {
    return new HomeSectionDefinition(
        id,
        title,
        subtitle,
        family,
        null,
        criteria,
        DEFAULT_CANDIDATE_LIMIT,
        DEFAULT_DISPLAY_LIMIT);
  }

  private HomeSectionDefinition semanticSection(
      String id, String title, String subtitle, MovieDiscoveryTheme theme) {
    return new HomeSectionDefinition(
        id,
        title,
        subtitle,
        HomeSectionFamily.THEME,
        theme,
        criteria(1980, null, null, null, Set.of(), MovieType.MOVIE, 6.5f, 0),
        DEFAULT_CANDIDATE_LIMIT,
        DEFAULT_DISPLAY_LIMIT);
  }

  private MovieDiscoveryTheme theme(String id, String prompt) {
    return new MovieDiscoveryTheme(id, prompt, 1);
  }

  private MovieDiscoveryCriteria criteria(
      Integer minYear,
      Integer maxYear,
      Integer minRuntime,
      Integer maxRuntime,
      Set<MovieGenre> genres,
      MovieType movieType,
      Float minRating,
      Integer minRatingCount) {
    return new MovieDiscoveryCriteria(
        minYear,
        maxYear,
        minRuntime,
        maxRuntime,
        genres,
        movieType,
        minRating,
        minRatingCount,
        Set.of());
  }
}
