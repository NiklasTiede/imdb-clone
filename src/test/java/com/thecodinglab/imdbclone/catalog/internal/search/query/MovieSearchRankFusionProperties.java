package com.thecodinglab.imdbclone.catalog.internal.search.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

class MovieSearchRankFusionProperties {

  private final MovieSearchRankFusion rankFusion = new MovieSearchRankFusion();

  @Property(tries = 300)
  void fullFusionContainsTheDistinctUnion(
      @ForAll("movieIds") List<Long> lexicalIds, @ForAll("movieIds") List<Long> semanticIds) {
    Set<Long> expectedIds = distinctUnion(lexicalIds, semanticIds);

    List<Long> fusedIds =
        ids(rankFusion.fuse(movies(lexicalIds), movies(semanticIds), 0, expectedIds.size() + 1));

    assertThat(fusedIds).containsExactlyInAnyOrderElementsOf(expectedIds).doesNotHaveDuplicates();
  }

  @Property(tries = 300)
  void paginationIsAWindowOverTheStableFullRanking(
      @ForAll("movieIds") List<Long> lexicalIds,
      @ForAll("movieIds") List<Long> semanticIds,
      @ForAll @IntRange(min = 0, max = 5) int page,
      @ForAll @IntRange(min = 1, max = 10) int size) {
    List<MovieSearchDocument> lexicalMovies = movies(lexicalIds);
    List<MovieSearchDocument> semanticMovies = movies(semanticIds);
    List<Long> fullRanking = ids(rankFusion.fuse(lexicalMovies, semanticMovies, 0, 100));

    int from = Math.min(page * size, fullRanking.size());
    int to = Math.min(from + size, fullRanking.size());

    assertThat(ids(rankFusion.fuse(lexicalMovies, semanticMovies, page, size)))
        .containsExactlyElementsOf(fullRanking.subList(from, to));
  }

  @Provide
  Arbitrary<List<Long>> movieIds() {
    return Arbitraries.integers()
        .between(1, 50)
        .map(Integer::longValue)
        .list()
        .ofMaxSize(20)
        .uniqueElements();
  }

  private Set<Long> distinctUnion(List<Long> first, List<Long> second) {
    return Stream.concat(first.stream(), second.stream())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private List<MovieSearchDocument> movies(List<Long> ids) {
    return ids.stream().map(this::movie).toList();
  }

  private List<Long> ids(List<MovieSearchDocument> movies) {
    return movies.stream().map(MovieSearchDocument::getId).toList();
  }

  private MovieSearchDocument movie(long id) {
    MovieSearchDocument movie = new MovieSearchDocument();
    movie.setId(id);
    return movie;
  }
}
