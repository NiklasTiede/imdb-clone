package com.thecodinglab.imdbclone.recommendation.internal.home;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.recommendation.api.HomeFeedRequest;
import com.thecodinglab.imdbclone.recommendation.api.HomeFeedResponse;
import com.thecodinglab.imdbclone.recommendation.api.HomeFeedSection;
import com.thecodinglab.imdbclone.recommendation.api.HomeFeedService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SeededHomeFeedService implements HomeFeedService {

  static final String STRATEGY_VERSION = "home-structured-v1";
  private static final int SECTIONS_PER_PAGE = 3;
  private static final int MAX_SECTIONS_PER_FEED = 24;

  private final HomeSectionCatalog sectionCatalog;
  private final HomeSectionComposer sectionComposer;

  SeededHomeFeedService(HomeSectionCatalog sectionCatalog, HomeSectionComposer sectionComposer) {
    this.sectionCatalog = sectionCatalog;
    this.sectionComposer = sectionComposer;
  }

  @Override
  public HomeFeedResponse homeFeed(HomeFeedRequest request) {
    String seed = HomeFeedSeed.canonical(request.seed(), request.feedInstanceId());
    int offset = HomeFeedCursor.decode(request.cursor());
    List<HomeSectionDefinition> definitions = sectionCatalog.ordered(seed);
    int boundedDefinitionCount = Math.min(MAX_SECTIONS_PER_FEED, definitions.size());
    if (offset > boundedDefinitionCount) {
      offset = boundedDefinitionCount;
    }

    Set<Long> seenMovieIds = new HashSet<>(request.excludedMovieIds());
    MovieRecord featuredMovie = null;
    if (offset == 0) {
      featuredMovie = sectionComposer.featured(sectionCatalog, seed, seenMovieIds);
    }

    List<HomeFeedSection> sections = new ArrayList<>();
    int inspected = offset;
    while (inspected < boundedDefinitionCount && sections.size() < SECTIONS_PER_PAGE) {
      HomeFeedSection section =
          sectionComposer.compose(definitions.get(inspected), seed, seenMovieIds);
      inspected++;
      if (section != null) {
        sections.add(section);
      }
    }

    boolean exhausted = inspected >= boundedDefinitionCount;
    return new HomeFeedResponse(
        seed,
        STRATEGY_VERSION,
        featuredMovie,
        List.copyOf(sections),
        exhausted ? null : HomeFeedCursor.encode(inspected),
        exhausted);
  }
}
