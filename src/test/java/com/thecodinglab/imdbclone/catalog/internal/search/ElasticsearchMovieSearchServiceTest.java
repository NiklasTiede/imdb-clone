package com.thecodinglab.imdbclone.catalog.internal.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElasticsearchMovieSearchServiceTest {

  @Mock private ElasticsearchClient esClient;
  @Mock private MovieEmbeddingClient movieEmbeddingClient;
  @Captor private ArgumentCaptor<SearchRequest> searchRequestCaptor;

  private final MovieSearchDocumentMapper mapper = new MovieSearchDocumentMapper();
  private final MovieSearchQueryBuilder queryBuilder = new MovieSearchQueryBuilder();

  @Test
  void searchMoviesSemantically_embedsQueryAndRunsKnnSearch() throws IOException {
    ElasticsearchMovieSearchService service =
        new ElasticsearchMovieSearchService(esClient, mapper, movieEmbeddingClient, queryBuilder);
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

  private SearchResponse<MovieSearchDocument> searchResponse(MovieSearchDocument document) {
    return SearchResponse.of(
        response ->
            response
                .took(1)
                .timedOut(false)
                .shards(shards -> shards.total(1).successful(1).failed(0))
                .hits(
                    hits ->
                        hits.total(total -> total.value(1).relation(TotalHitsRelation.Eq))
                            .hits(
                                List.of(
                                    Hit.<MovieSearchDocument>of(
                                        hit -> hit.index("movies").id("7").source(document))))));
  }
}
