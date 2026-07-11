package com.thecodinglab.imdbclone.catalog.internal.search;

import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCandidateProvider;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCriteria;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentMapper;
import com.thecodinglab.imdbclone.catalog.internal.search.query.MovieSearchQueryBuilder;
import com.thecodinglab.imdbclone.shared.error.OpenSearchOperationException;
import java.io.IOException;
import java.util.List;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;

@Service
public class OpenSearchMovieDiscoveryCandidates implements MovieDiscoveryCandidateProvider {

  private static final String MOVIES_INDEX = "movies";

  private final OpenSearchClient openSearchClient;
  private final MovieSearchDocumentMapper movieSearchDocumentMapper;
  private final MovieSearchQueryBuilder movieSearchQueryBuilder;

  public OpenSearchMovieDiscoveryCandidates(
      OpenSearchClient openSearchClient,
      MovieSearchDocumentMapper movieSearchDocumentMapper,
      MovieSearchQueryBuilder movieSearchQueryBuilder) {
    this.openSearchClient = openSearchClient;
    this.movieSearchDocumentMapper = movieSearchDocumentMapper;
    this.movieSearchQueryBuilder = movieSearchQueryBuilder;
  }

  @Override
  public List<MovieRecord> findCandidates(MovieDiscoveryCriteria criteria, int candidateLimit) {
    SearchRequest request =
        movieSearchQueryBuilder.buildDiscoveryCandidateSearchRequest(
            MOVIES_INDEX, criteria, candidateLimit);
    return execute(request, candidateLimit);
  }

  @Override
  public List<MovieRecord> findSemanticCandidates(
      MovieDiscoveryCriteria criteria, float[] themeEmbedding, int candidateLimit) {
    if (themeEmbedding == null || themeEmbedding.length == 0) {
      return List.of();
    }
    SearchRequest request =
        movieSearchQueryBuilder.buildSemanticDiscoveryCandidateSearchRequest(
            MOVIES_INDEX, themeEmbedding, criteria, candidateLimit);
    return execute(request, candidateLimit);
  }

  private List<MovieRecord> execute(SearchRequest request, int candidateLimit) {
    if (candidateLimit < 1) {
      throw new IllegalArgumentException("candidateLimit must be positive");
    }
    try {
      SearchResponse<MovieSearchDocument> response =
          openSearchClient.search(request, MovieSearchDocument.class);
      return response.hits().hits().stream()
          .map(Hit::source)
          .filter(document -> document != null && document.getId() != null)
          .map(movieSearchDocumentMapper::toMovieRecord)
          .toList();
    } catch (IOException | RuntimeException ex) {
      throw new OpenSearchOperationException(
          "Error while retrieving movie discovery candidates", ex);
    }
  }
}
