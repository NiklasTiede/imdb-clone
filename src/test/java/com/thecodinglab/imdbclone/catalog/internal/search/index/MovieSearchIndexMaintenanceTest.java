package com.thecodinglab.imdbclone.catalog.internal.search.index;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.internal.search.projection.MovieSearchProjectionWork;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.data.core.OpenSearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

@ExtendWith(MockitoExtension.class)
class MovieSearchIndexMaintenanceTest {
  @Mock private MovieSearchDocumentRepository documents;
  @Mock private OpenSearchOperations operations;
  @Mock private MovieSearchProjectionWork work;
  @Mock private IndexOperations index;
  private MovieSearchIndexMaintenance maintenance;

  @BeforeEach
  void setUp() {
    maintenance = new MovieSearchIndexMaintenance(documents, operations, work);
    when(operations.indexOps(MovieSearchDocument.class)).thenReturn(index);
  }

  @Test
  void missingIndexIsCreatedUnderTheExclusiveWriterLock() {
    when(index.exists()).thenReturn(false);
    maintenance.resetMoviesIndex();
    var order = inOrder(work, index, documents);
    order.verify(work).lockIndexForReset();
    order.verify(index).create(any(), any());
    order.verify(documents).deleteAll();
  }

  @Test
  void currentMappingIsKept() {
    when(index.exists()).thenReturn(true);
    when(index.getMapping())
        .thenReturn(
            Map.of(
                "properties",
                Map.of(
                    "primaryTitle", Map.of("type", "search_as_you_type"),
                    "originalTitle", Map.of("type", "search_as_you_type"),
                    "embedding", Map.of("type", "knn_vector"))));
    maintenance.resetMoviesIndex();
    verify(index, never()).delete();
    verify(index, never()).create(any(), any());
    verify(documents).deleteAll();
  }

  @Test
  void staleMappingIsRecreated() {
    when(index.exists()).thenReturn(true);
    when(index.getMapping())
        .thenReturn(
            Map.of(
                "properties",
                Map.of(
                    "primaryTitle",
                    Map.of("type", "text"),
                    "originalTitle",
                    Map.of("type", "text"))));
    maintenance.resetMoviesIndex();
    var order = inOrder(index, documents);
    order.verify(index).delete();
    order.verify(index).create(any(), any());
    order.verify(documents).deleteAll();
  }
}
