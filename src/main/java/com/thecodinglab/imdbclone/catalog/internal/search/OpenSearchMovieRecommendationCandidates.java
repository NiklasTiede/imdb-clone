package com.thecodinglab.imdbclone.catalog.internal.search;

import com.thecodinglab.imdbclone.catalog.api.MovieRecommendationCandidate;
import com.thecodinglab.imdbclone.catalog.api.MovieRecommendationCandidateProvider;
import com.thecodinglab.imdbclone.catalog.api.MovieRecommendationCandidates;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieReferenceService;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentMapper;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentRepository;
import com.thecodinglab.imdbclone.catalog.internal.search.query.MovieSearchQueryBuilder;
import com.thecodinglab.imdbclone.shared.error.OpenSearchOperationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpenSearchMovieRecommendationCandidates
    implements MovieRecommendationCandidateProvider {

  private static final Logger logger =
      LoggerFactory.getLogger(OpenSearchMovieRecommendationCandidates.class);
  private static final String MOVIES_INDEX = "movies";

  private final MovieReferenceService movieReferenceService;
  private final MovieSearchDocumentRepository movieSearchDocumentRepository;
  private final OpenSearchClient openSearchClient;
  private final MovieSearchDocumentMapper movieSearchDocumentMapper;
  private final MovieSearchQueryBuilder movieSearchQueryBuilder;

  public OpenSearchMovieRecommendationCandidates(
      MovieReferenceService movieReferenceService,
      MovieSearchDocumentRepository movieSearchDocumentRepository,
      OpenSearchClient openSearchClient,
      MovieSearchDocumentMapper movieSearchDocumentMapper,
      MovieSearchQueryBuilder movieSearchQueryBuilder) {
    this.movieReferenceService = movieReferenceService;
    this.movieSearchDocumentRepository = movieSearchDocumentRepository;
    this.openSearchClient = openSearchClient;
    this.movieSearchDocumentMapper = movieSearchDocumentMapper;
    this.movieSearchQueryBuilder = movieSearchQueryBuilder;
  }

  @Override
  public MovieRecommendationCandidates findCandidates(Long movieId, int candidateLimit) {
    if (candidateLimit < 1) {
      throw new IllegalArgumentException("candidateLimit must be positive");
    }

    MovieRecord anchor = movieReferenceService.findMovieById(movieId);
    try {
      MovieSearchDocument anchorDocument =
          movieSearchDocumentRepository.findById(movieId).orElse(null);
      if (anchorDocument == null || !hasEmbedding(anchorDocument)) {
        logger.warn(
            "No recommendation candidates were generated because movieId [{}] has no indexed embedding",
            movieId);
        return new MovieRecommendationCandidates(anchor, List.of());
      }

      SearchRequest request =
          movieSearchQueryBuilder.buildRecommendationCandidateSearchRequest(
              MOVIES_INDEX, anchorDocument.getEmbedding(), movieId, candidateLimit);
      SearchResponse<MovieSearchDocument> response =
          openSearchClient.search(request, MovieSearchDocument.class);

      List<MovieRecommendationCandidate> candidates = new ArrayList<>();
      for (Hit<MovieSearchDocument> hit : response.hits().hits()) {
        MovieSearchDocument document = hit.source();
        if (document == null || movieId.equals(document.getId())) {
          continue;
        }
        candidates.add(
            new MovieRecommendationCandidate(
                movieSearchDocumentMapper.toMovieRecord(document), candidates.size() + 1));
      }
      return new MovieRecommendationCandidates(anchor, List.copyOf(candidates));
    } catch (IOException | RuntimeException ex) {
      throw new OpenSearchOperationException(
          "Error while retrieving movie recommendation candidates", ex);
    }
  }

  private boolean hasEmbedding(MovieSearchDocument document) {
    return document.getEmbedding() != null && document.getEmbedding().length > 0;
  }
}
