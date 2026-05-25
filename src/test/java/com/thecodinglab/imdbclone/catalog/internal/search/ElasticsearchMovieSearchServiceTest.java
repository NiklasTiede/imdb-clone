package com.thecodinglab.imdbclone.catalog.internal.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.catalog.internal.search.embedding.MovieEmbeddingClient;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentMapper;
import com.thecodinglab.imdbclone.catalog.internal.search.query.MovieSearchQueryBuilder;
import com.thecodinglab.imdbclone.catalog.internal.search.query.MovieSearchRankFusion;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
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

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ElasticsearchMovieSearchServiceTest {

  @Mock private ElasticsearchClient esClient;
  @Mock private MovieEmbeddingClient movieEmbeddingClient;
  @Captor private ArgumentCaptor<SearchRequest> searchRequestCaptor;

  private final MovieSearchDocumentMapper mapper = new MovieSearchDocumentMapper();
  private final MovieSearchQueryBuilder queryBuilder = new MovieSearchQueryBuilder();
  private final MovieSearchRankFusion rankFusion = new MovieSearchRankFusion();

  @Test
  void searchMoviesSemantically_embedsQueryAndRunsKnnSearch() throws IOException {
    ElasticsearchMovieSearchService service = searchService();
    MovieSearchRequest request = new MovieSearchRequest(null, null, null, null, Set.of(), null);
    MovieSearchDocument document = new MovieSearchDocument();
    document.setId(7L);
    document.setPrimaryTitle("Alien");
    document.setOriginalTitle("Alien");
    when(movieEmbeddingClient.embedText("space horror movie")).thenReturn(new float[] {0.1f, 0.2f});
    when(esClient.search(searchRequestCaptor.capture(), eq(MovieSearchDocument.class)))
        .thenReturn(searchResponse(document));

    PagedResponse<MovieRecord> response =
        service.searchMoviesSemantically("space horror movie", request, 0, 20);

    verify(movieEmbeddingClient).embedText("space horror movie");
    SearchRequest searchRequest = searchRequestCaptor.getValue();
    assertThat(searchRequest.knn()).hasSize(1);
    assertThat(searchRequest.knn().getFirst().field()).isEqualTo("embedding");
    assertThat(searchRequest.knn().getFirst().queryVector()).containsExactly(0.1f, 0.2f);
    assertThat(response.getTotalElements()).isEqualTo(1);
    assertThat(response.getContent())
        .extracting(MovieRecord::primaryTitle)
        .containsExactly("Alien");
  }

  @Test
  void searchMovies_withTextQueryRunsLexicalAndSemanticSearchAndFusesResults() throws IOException {
    ElasticsearchMovieSearchService service = searchService();
    MovieSearchRequest request = new MovieSearchRequest(null, null, null, null, Set.of(), null);
    when(movieEmbeddingClient.embedText("space horror")).thenReturn(new float[] {0.1f, 0.2f});
    when(esClient.search(searchRequestCaptor.capture(), eq(MovieSearchDocument.class)))
        .thenReturn(
            searchResponse(List.of(movie(2, "Lexical First"), movie(1, "Shared Match"))),
            searchResponse(List.of(movie(1, "Shared Match"), movie(3, "Semantic Only"))));

    PagedResponse<MovieRecord> response = service.searchMovies("space horror", request, 0, 20);

    verify(movieEmbeddingClient).embedText("space horror");
    List<SearchRequest> searchRequests = searchRequestCaptor.getAllValues();
    assertThat(searchRequests).hasSize(2);
    assertThat(searchRequests.getFirst().query()).isNotNull();
    assertThat(searchRequests.getFirst().knn()).isEmpty();
    assertThat(searchRequests.getLast().knn()).hasSize(1);
    assertThat(response.getContent()).extracting(MovieRecord::id).containsExactly(1L, 2L, 3L);
  }

  @Test
  void searchMovies_withTextQueryCapsHybridResultsToFourPages() throws IOException {
    ElasticsearchMovieSearchService service = searchService();
    MovieSearchRequest request = new MovieSearchRequest(null, null, null, null, Set.of(), null);
    List<MovieSearchDocument> candidates =
        LongStream.rangeClosed(1, 100).mapToObj(id -> movie(id, "Movie " + id)).toList();
    when(movieEmbeddingClient.embedText("space horror")).thenReturn(new float[] {0.1f, 0.2f});
    when(esClient.search(searchRequestCaptor.capture(), eq(MovieSearchDocument.class)))
        .thenReturn(searchResponse(candidates), searchResponse(candidates));

    PagedResponse<MovieRecord> response = service.searchMovies("space horror", request, 0, 20);

    assertThat(response.getTotalElements()).isEqualTo(80);
    assertThat(response.getTotalPages()).isEqualTo(4);
    assertThat(response.getContent()).hasSize(20);
  }

  @Test
  void searchMovies_withBlankQueryKeepsLexicalFilterOnlySearch() throws IOException {
    ElasticsearchMovieSearchService service = searchService();
    MovieSearchRequest request = new MovieSearchRequest(null, null, null, null, Set.of(), null);
    when(esClient.search(searchRequestCaptor.capture(), eq(MovieSearchDocument.class)))
        .thenReturn(searchResponse(List.of(movie(5, "Filter Match"))));

    PagedResponse<MovieRecord> response = service.searchMovies(" ", request, 0, 20);

    verify(movieEmbeddingClient, never()).embedText(" ");
    assertThat(searchRequestCaptor.getAllValues()).hasSize(1);
    assertThat(searchRequestCaptor.getValue().query()).isNotNull();
    assertThat(searchRequestCaptor.getValue().knn()).isEmpty();
    assertThat(response.getContent()).extracting(MovieRecord::id).containsExactly(5L);
  }

  private SearchResponse<MovieSearchDocument> searchResponse(MovieSearchDocument document) {
    return searchResponse(List.of(document));
  }

  private ElasticsearchMovieSearchService searchService() {
    return new ElasticsearchMovieSearchService(
        esClient, mapper, movieEmbeddingClient, queryBuilder, rankFusion);
  }

  private SearchResponse<MovieSearchDocument> searchResponse(List<MovieSearchDocument> documents) {
    return searchResponse(documents, documents.size());
  }

  private SearchResponse<MovieSearchDocument> searchResponse(
      List<MovieSearchDocument> documents, long totalHits) {
    return SearchResponse.of(
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
