package com.thecodinglab.imdbclone.catalog.internal.search.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

class MovieSearchQueryIntentClassifierTest {

  private final MovieSearchQueryIntentClassifier classifier =
      new MovieSearchQueryIntentClassifier();

  @Test
  void identifiesExactPrefixAndContainedTitleQueries() {
    assertThat(classifier.isConfidentTitleQuery("Spirited Away", movies("Spirited Away"))).isTrue();
    assertThat(classifier.isConfidentTitleQuery("spir", movies("Spirited Away"))).isTrue();
    assertThat(classifier.isConfidentTitleQuery("quiet place", movies("A Quiet Place"))).isTrue();
  }

  @Test
  void treatsDescriptionsThemesAndTyposAsSemanticQueries() {
    assertThat(
            classifier.isConfidentTitleQuery(
                "dream heist inside people's minds", movies("Requiem for a Dream")))
        .isFalse();
    assertThat(classifier.isConfidentTitleQuery("space horror", movies("2001: A Space Odyssey")))
        .isFalse();
    assertThat(classifier.isConfidentTitleQuery("sprited away", movies("Cast Away"))).isFalse();
  }

  @Test
  void keepsVeryShortInputOnTheFastLexicalPath() {
    assertThat(classifier.isConfidentTitleQuery("up", List.of())).isTrue();
  }

  private List<MovieSearchDocument> movies(String title) {
    MovieSearchDocument movie = new MovieSearchDocument();
    movie.setId(1L);
    movie.setPrimaryTitle(title);
    movie.setOriginalTitle(title);
    return List.of(movie);
  }
}
