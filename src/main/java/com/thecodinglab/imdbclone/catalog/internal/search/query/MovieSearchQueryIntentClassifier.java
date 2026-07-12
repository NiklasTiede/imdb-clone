package com.thecodinglab.imdbclone.catalog.internal.search.query;

import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class MovieSearchQueryIntentClassifier {

  private static final int MINIMUM_SEMANTIC_QUERY_LENGTH = 3;

  public boolean isConfidentTitleQuery(String query, List<MovieSearchDocument> lexicalCandidates) {
    String normalizedQuery = normalize(query);
    if (normalizedQuery.length() < MINIMUM_SEMANTIC_QUERY_LENGTH) {
      return true;
    }
    if (lexicalCandidates == null || lexicalCandidates.isEmpty()) {
      return false;
    }

    MovieSearchDocument firstCandidate = lexicalCandidates.getFirst();
    return titleContainsQuery(firstCandidate.getPrimaryTitle(), normalizedQuery)
        || titleContainsQuery(firstCandidate.getOriginalTitle(), normalizedQuery);
  }

  private boolean titleContainsQuery(String title, String normalizedQuery) {
    String normalizedTitle = normalize(title);
    return normalizedTitle.equals(normalizedQuery)
        || normalizedTitle.startsWith(normalizedQuery)
        || normalizedTitle.contains(" " + normalizedQuery);
  }

  private String normalize(String value) {
    if (value == null) {
      return "";
    }
    return Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", " ")
        .trim()
        .replaceAll("\\s+", " ");
  }
}
