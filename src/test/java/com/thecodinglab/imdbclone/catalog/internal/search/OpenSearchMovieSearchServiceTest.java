package com.thecodinglab.imdbclone.catalog.internal.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.catalog.internal.search.embedding.MovieEmbeddingClient;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentMapper;
import com.thecodinglab.imdbclone.catalog.internal.search.query.MovieSearchQueryBuilder;
import com.thecodinglab.imdbclone.catalog.internal.search.query.MovieSearchRankFusion;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import com.thecodinglab.imdbclone.shared.error.OpenSearchOperationException;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
@SuppressWarnings("unchecked")
class OpenSearchMovieSearchServiceTest {

  @Mock private OpenSearchClient openSearchClient;
  @Mock private MovieEmbeddingClient movieEmbeddingClient;
  @Captor private ArgumentCaptor<SearchRequest> searchRequestCaptor;

  private final MovieSearchDocumentMapper mapper = new MovieSearchDocumentMapper();
  private final MovieSearchQueryBuilder queryBuilder = new MovieSearchQueryBuilder();
  private final MovieSearchRankFusion rankFusion = new MovieSearchRankFusion();

  @Test
  void searchMoviesSemantically_embedsQueryAndRunsKnnSearch(CapturedOutput output)
      throws IOException {
    OpenSearchMovieSearchService service = searchService();
    MovieSearchRequest request = new MovieSearchRequest(null, null, null, null, Set.of(), null);
    MovieSearchDocument document = new MovieSearchDocument();
    document.setId(7L);
    document.setPrimaryTitle("Alien");
    document.setOriginalTitle("Alien");
    when(movieEmbeddingClient.embedText("space horror movie")).thenReturn(new float[] {0.1f, 0.2f});
    when(openSearchClient.search(searchRequestCaptor.capture(), eq(MovieSearchDocument.class)))
        .thenReturn(searchResponse(document));

    PagedResponse<MovieRecord> response =
        service.searchMoviesSemantically("space horror movie", request, 0, 20);

    verify(movieEmbeddingClient).embedText("space horror movie");
    SearchRequest searchRequest = searchRequestCaptor.getValue();
    assertThat(searchRequest.query()).isNotNull();
    assertThat(searchRequest.query().isKnn()).isTrue();
    assertThat(searchRequest.query().knn().field()).isEqualTo("embedding");
    assertThat(searchRequest.query().knn().vector()).containsExactly(0.1f, 0.2f);
    assertThat(response.getTotalElements()).isEqualTo(1);
    assertThat(response.getContent())
        .extracting(MovieRecord::primaryTitle)
        .containsExactly("Alien");
    assertThat(output.getOut())
        .doesNotContain("Semantic movie search completed")
        .doesNotContain("Semantic movie search query json:");
  }

  @Test
  void searchMovies_withTextQueryRunsLexicalAndSemanticSearchAndFusesResults(CapturedOutput output)
      throws IOException {
    OpenSearchMovieSearchService service = searchService();
    MovieSearchRequest request = new MovieSearchRequest(null, null, null, null, Set.of(), null);
    when(movieEmbeddingClient.embedText("space horror")).thenReturn(new float[] {0.1f, 0.2f});
    when(openSearchClient.search(searchRequestCaptor.capture(), eq(MovieSearchDocument.class)))
        .thenReturn(
            searchResponse(List.of(movie(2, "Lexical First"), movie(1, "Shared Match"))),
            searchResponse(List.of(movie(1, "Shared Match"), movie(3, "Semantic Only"))));

    PagedResponse<MovieRecord> response = service.searchMovies("space horror", request, 0, 20);

    verify(movieEmbeddingClient).embedText("space horror");
    List<SearchRequest> searchRequests = searchRequestCaptor.getAllValues();
    assertThat(searchRequests).hasSize(2);
    assertThat(searchRequests.getFirst().query()).isNotNull();
    assertThat(searchRequests.getFirst().query().isKnn()).isFalse();
    assertThat(searchRequests.getLast().query().isKnn()).isTrue();
    assertThat(response.getContent()).extracting(MovieRecord::id).containsExactly(1L, 2L, 3L);
    assertThat(output.getOut())
        .doesNotContain("Hybrid movie search completed")
        .doesNotContain("Hybrid lexical movie search query json:")
        .doesNotContain("Hybrid semantic movie search query json:");
  }

  @Test
  void searchMovies_withTextQueryCapsHybridResultsToFourPages() throws IOException {
    OpenSearchMovieSearchService service = searchService();
    MovieSearchRequest request = new MovieSearchRequest(null, null, null, null, Set.of(), null);
    List<MovieSearchDocument> candidates =
        LongStream.rangeClosed(1, 100).mapToObj(id -> movie(id, "Movie " + id)).toList();
    when(movieEmbeddingClient.embedText("space horror")).thenReturn(new float[] {0.1f, 0.2f});
    when(openSearchClient.search(searchRequestCaptor.capture(), eq(MovieSearchDocument.class)))
        .thenReturn(searchResponse(candidates), searchResponse(candidates));

    PagedResponse<MovieRecord> response = service.searchMovies("space horror", request, 0, 20);

    assertThat(response.getTotalElements()).isEqualTo(80);
    assertThat(response.getTotalPages()).isEqualTo(4);
    assertThat(response.getContent()).hasSize(20);
  }

  @Test
  void searchMovies_withBlankQueryKeepsLexicalFilterOnlySearch(CapturedOutput output)
      throws IOException {
    OpenSearchMovieSearchService service = searchService();
    MovieSearchRequest request = new MovieSearchRequest(null, null, null, null, Set.of(), null);
    when(openSearchClient.search(searchRequestCaptor.capture(), eq(MovieSearchDocument.class)))
        .thenReturn(searchResponse(List.of(movie(5, "Filter Match"))));

    PagedResponse<MovieRecord> response = service.searchMovies(" ", request, 0, 20);

    verify(movieEmbeddingClient, never()).embedText(" ");
    assertThat(searchRequestCaptor.getAllValues()).hasSize(1);
    assertThat(searchRequestCaptor.getValue().query()).isNotNull();
    assertThat(searchRequestCaptor.getValue().query().isKnn()).isFalse();
    assertThat(response.getContent()).extracting(MovieRecord::id).containsExactly(5L);
    assertThat(output.getOut())
        .doesNotContain("Movie search completed")
        .doesNotContain("Document search query json:")
        .doesNotContain("Scores of found documents:");
  }

  @Test
  void searchMovies_wrapsOpenSearchQueryFailures() throws IOException {
    OpenSearchMovieSearchService service = searchService();
    MovieSearchRequest request = new MovieSearchRequest(null, null, null, null, Set.of(), null);
    OpenSearchException failure =
        new OpenSearchException(
            ErrorResponse.of(
                response ->
                    response
                        .status(400)
                        .error(error -> error.type("query_shard_exception").reason("bad query"))));
    when(openSearchClient.search(searchRequestCaptor.capture(), eq(MovieSearchDocument.class)))
        .thenThrow(failure);

    assertThatThrownBy(() -> service.searchMovies(" ", request, 0, 20))
        .isInstanceOf(OpenSearchOperationException.class)
        .hasCause(failure)
        .hasMessage("error while search was performed");
  }

  private SearchResponse<MovieSearchDocument> searchResponse(MovieSearchDocument document) {
    return searchResponse(List.of(document));
  }

  private OpenSearchMovieSearchService searchService() {
    return new OpenSearchMovieSearchService(
        openSearchClient, mapper, movieEmbeddingClient, queryBuilder, rankFusion);
  }

  private SearchResponse<MovieSearchDocument> searchResponse(List<MovieSearchDocument> documents) {
    return searchResponse(documents, documents.size());
  }

  private SearchResponse<MovieSearchDocument> searchResponse(
      List<MovieSearchDocument> documents, long totalHits) {
    return SearchResponse.searchResponseOf(
        response ->
            response
                .took(1)
                .timedOut(false)
                .shards(shards -> shards.total(1).successful(1).failed(0))
                .hits(
                    hits ->
                        hits.total(total -> total.value(totalHits).relation(TotalHitsRelation.Eq))
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

  private MovieSearchDocument movie(long id, String title) {
    MovieSearchDocument document = new MovieSearchDocument();
    document.setId(id);
    document.setPrimaryTitle(title);
    document.setOriginalTitle(title);
    return document;
  }
}
