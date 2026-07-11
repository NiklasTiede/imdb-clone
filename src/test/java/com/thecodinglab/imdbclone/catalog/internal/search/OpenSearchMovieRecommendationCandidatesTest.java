package com.thecodinglab.imdbclone.catalog.internal.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.api.MovieRecommendationCandidates;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieReferenceService;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentMapper;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentRepository;
import com.thecodinglab.imdbclone.catalog.internal.search.query.MovieSearchQueryBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;

@ExtendWith(MockitoExtension.class)
class OpenSearchMovieRecommendationCandidatesTest {

  @Mock private MovieReferenceService movieReferenceService;
  @Mock private MovieSearchDocumentRepository movieSearchDocumentRepository;
  @Mock private OpenSearchClient openSearchClient;
  @Captor private ArgumentCaptor<SearchRequest> requestCaptor;

  private final MovieSearchDocumentMapper mapper = new MovieSearchDocumentMapper();
  private final MovieSearchQueryBuilder queryBuilder = new MovieSearchQueryBuilder();

  @Test
  void findCandidates_reusesIndexedEmbeddingAndPreservesSemanticRank() throws IOException {
    MovieRecord anchor = movieRecord(1L, "Anchor");
    MovieSearchDocument anchorDocument = movieDocument(1L, "Anchor");
    anchorDocument.setEmbedding(new float[] {0.1f, 0.2f});
    MovieSearchDocument first = movieDocument(2L, "First");
    MovieSearchDocument second = movieDocument(3L, "Second");
    when(movieReferenceService.findMovieById(1L)).thenReturn(anchor);
    when(movieSearchDocumentRepository.findById(1L)).thenReturn(Optional.of(anchorDocument));
    when(openSearchClient.search(requestCaptor.capture(), eq(MovieSearchDocument.class)))
        .thenReturn(searchResponse(List.of(first, anchorDocument, second)));

    MovieRecommendationCandidates result = provider().findCandidates(1L, 24);

    assertThat(requestCaptor.getValue().query().knn().vector()).containsExactly(0.1f, 0.2f);
    assertThat(result.anchor()).isEqualTo(anchor);
    assertThat(result.candidates())
        .extracting(candidate -> candidate.movie().id())
        .containsExactly(2L, 3L);
    assertThat(result.candidates())
        .extracting(candidate -> candidate.semanticRank())
        .containsExactly(1, 2);
  }

  @Test
  void findCandidates_returnsEmptyWhenProjectionHasNoEmbedding() throws IOException {
    MovieRecord anchor = movieRecord(1L, "Anchor");
    MovieSearchDocument anchorDocument = movieDocument(1L, "Anchor");
    when(movieReferenceService.findMovieById(1L)).thenReturn(anchor);
    when(movieSearchDocumentRepository.findById(1L)).thenReturn(Optional.of(anchorDocument));

    MovieRecommendationCandidates result = provider().findCandidates(1L, 24);

    assertThat(result.anchor()).isEqualTo(anchor);
    assertThat(result.candidates()).isEmpty();
    verify(openSearchClient, never())
        .search(requestCaptor.capture(), eq(MovieSearchDocument.class));
  }

  private OpenSearchMovieRecommendationCandidates provider() {
    return new OpenSearchMovieRecommendationCandidates(
        movieReferenceService,
        movieSearchDocumentRepository,
        openSearchClient,
        mapper,
        queryBuilder);
  }

  private SearchResponse<MovieSearchDocument> searchResponse(List<MovieSearchDocument> documents) {
    return SearchResponse.searchResponseOf(
        response ->
            response
                .took(1)
                .timedOut(false)
                .shards(shards -> shards.total(1).successful(1).failed(0))
                .hits(
                    hits ->
                        hits.total(
                                total ->
                                    total.value(documents.size()).relation(TotalHitsRelation.Eq))
                            .hits(
                                documents.stream()
                                    .map(
                                        document ->
                                            Hit.<MovieSearchDocument>of(
                                                hit ->
                                                    hit.index("movies")
                                                        .id(document.getId().toString())
                                                        .source(document)))
                                    .toList())));
  }

  private MovieSearchDocument movieDocument(Long id, String title) {
    MovieSearchDocument document = new MovieSearchDocument();
    document.setId(id);
    document.setPrimaryTitle(title);
    document.setOriginalTitle(title);
    return document;
  }

  private MovieRecord movieRecord(Long id, String title) {
    return new MovieRecord(
        id, null, null, null, title, title, false, 2000, null, 120, null, null, null, 8.0f, 10_000,
        null, null, null, null, null, null);
  }
}
