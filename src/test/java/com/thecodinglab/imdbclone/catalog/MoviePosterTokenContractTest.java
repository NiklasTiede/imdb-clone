package com.thecodinglab.imdbclone.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class MoviePosterTokenContractTest {

  @Test
  void movieApiUsesPosterImageTokenInsteadOfLegacyImageUrlToken() {
    assertThat(MovieRecord.class.getRecordComponents())
        .extracting(component -> component.getName())
        .contains("posterImageToken")
        .doesNotContain("imageUrlToken");
  }

  @Test
  void moviePersistenceDoesNotExposeLegacyImageUrlTokenAccessors() {
    assertThat(accessorNames(Movie.class))
        .doesNotContain("getImageUrlToken", "setImageUrlToken")
        .contains("getPosterImageToken", "setPosterImageToken");
  }

  @Test
  void movieSearchDocumentDoesNotStoreLegacyImageUrlToken() {
    assertThat(accessorNames(MovieSearchDocument.class))
        .doesNotContain("getImageUrlToken", "setImageUrlToken")
        .contains("getPosterImageToken", "setPosterImageToken");
  }

  private static Iterable<String> accessorNames(Class<?> type) {
    return java.util.Arrays.stream(type.getDeclaredMethods()).map(Method::getName).toList();
  }
}
