package com.thecodinglab.imdbclone.catalog.internal.search.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.data.core.OpenSearchOperations;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.IndexOperations;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class MovieSearchIndexMaintenanceTest {

  @Mock private MovieRepository movieRepository;
  @Mock private MovieSearchDocumentRepository movieSearchRepository;
  @Mock private MovieSearchDocumentMapper movieSearchDocumentMapper;
  @Mock private MovieSearchEmbeddingProjector movieSearchEmbeddingProjector;
  @Mock private OpenSearchOperations openSearchOperations;
  @Mock private IndexOperations indexOperations;

  private MovieSearchIndexMaintenance maintenance;

  @BeforeEach
  void setUp() {
    maintenance =
        new MovieSearchIndexMaintenance(
            movieRepository,
            movieSearchRepository,
            movieSearchDocumentMapper,
            movieSearchEmbeddingProjector,
            openSearchOperations);
  }

  @Test
  void reindexMoviesCreatesTheIndexWhenMissingAndIndexesEveryMovie() {
    Movie firstMovie = new Movie("First", "First", null, 90);
    Movie secondMovie = new Movie("Second", "Second", null, 95);
    MovieSearchDocument firstDocument = new MovieSearchDocument();
    MovieSearchDocument secondDocument = new MovieSearchDocument();
    PageRequest firstPage = PageRequest.of(0, 500, Sort.by("id").ascending());
    PageRequest secondPage = PageRequest.of(1, 500, Sort.by("id").ascending());

    when(openSearchOperations.indexOps(MovieSearchDocument.class)).thenReturn(indexOperations);
    when(indexOperations.exists()).thenReturn(false);
    when(movieRepository.findAll(firstPage))
        .thenReturn(new PageImpl<>(List.of(firstMovie), firstPage, 501));
    when(movieRepository.findAll(secondPage))
        .thenReturn(new PageImpl<>(List.of(secondMovie), secondPage, 501));
    when(movieSearchDocumentMapper.toDocument(firstMovie)).thenReturn(firstDocument);
    when(movieSearchDocumentMapper.toDocument(secondMovie)).thenReturn(secondDocument);

    long indexedMovies = maintenance.reindexMovies();

    assertThat(indexedMovies).isEqualTo(2);
    verify(indexOperations).create(any(), any());
    verify(movieSearchRepository).deleteAll();
    verify(movieSearchEmbeddingProjector).addEmbedding(firstMovie, firstDocument);
    verify(movieSearchEmbeddingProjector).addEmbedding(secondMovie, secondDocument);
    verify(movieSearchRepository).saveAll(List.of(firstDocument));
    verify(movieSearchRepository).saveAll(List.of(secondDocument));
  }

  @Test
  void reindexMoviesLogsBatchProgress(CapturedOutput output) {
    List<Movie> movies =
        LongStream.rangeClosed(1, 11)
            .mapToObj(index -> new Movie("Movie " + index, "Movie " + index, null, 90))
            .toList();
    List<MovieSearchDocument> documents =
        LongStream.rangeClosed(1, 11).mapToObj(ignored -> new MovieSearchDocument()).toList();
    PageRequest firstPage = PageRequest.of(0, 500, Sort.by("id").ascending());

    when(openSearchOperations.indexOps(MovieSearchDocument.class)).thenReturn(indexOperations);
    when(indexOperations.exists()).thenReturn(false);
    when(movieRepository.findAll(firstPage)).thenReturn(new PageImpl<>(movies, firstPage, 11));
    for (int index = 0; index < movies.size(); index++) {
      when(movieSearchDocumentMapper.toDocument(movies.get(index)))
          .thenReturn(documents.get(index));
    }

    maintenance.reindexMovies();

    assertThat(output.getOut())
        .doesNotContain("Starting movie search reindex")
        .doesNotContain("Creating missing OpenSearch movies index")
        .contains(
            "Embedded movie search documents page=0 embeddedInPage=10 batchSize=11 totalEmbedded=10")
        .contains("intervalSize=10")
        .contains(
            "Embedded movie search documents page=0 embeddedInPage=11 batchSize=11 totalEmbedded=11")
        .contains("intervalSize=1")
        .contains("averageMovieEmbeddingDurationMs=")
        .contains("Indexed movie search batch page=0 batchSize=11 totalIndexed=11")
        .contains("Finished movie search reindex totalIndexed=11");
  }

  @Test
  void reindexMoviesKeepsExistingIndexMapping() {
    PageRequest firstPage = PageRequest.of(0, 500, Sort.by("id").ascending());

    when(openSearchOperations.indexOps(MovieSearchDocument.class)).thenReturn(indexOperations);
    when(indexOperations.exists()).thenReturn(true);
    when(indexOperations.getMapping()).thenReturn(searchAsYouTypeTitleMapping());
    when(movieRepository.findAll(firstPage)).thenReturn(new PageImpl<>(List.of(), firstPage, 0));

    long indexedMovies = maintenance.reindexMovies();

    assertThat(indexedMovies).isZero();
    verify(indexOperations, never()).delete();
    verify(indexOperations, never()).create(any(), any());
    verify(movieSearchRepository, never()).saveAll(any());
  }

  @Test
  void reindexMoviesRecreatesExistingIndexWhenTitleMappingIsStale() {
    PageRequest firstPage = PageRequest.of(0, 500, Sort.by("id").ascending());

    when(openSearchOperations.indexOps(MovieSearchDocument.class)).thenReturn(indexOperations);
    when(indexOperations.exists()).thenReturn(true);
    when(indexOperations.getMapping()).thenReturn(textTitleMapping());
    when(movieRepository.findAll(firstPage)).thenReturn(new PageImpl<>(List.of(), firstPage, 0));

    long indexedMovies = maintenance.reindexMovies();

    assertThat(indexedMovies).isZero();
    verify(indexOperations).delete();
    verify(indexOperations).create(any(), any());
    verify(movieSearchRepository).deleteAll();
    verify(movieSearchRepository, never()).saveAll(any());
  }

  private static Map<String, Object> searchAsYouTypeTitleMapping() {
    return Map.of(
        "properties",
        Map.of(
            "primaryTitle", Map.of("type", "search_as_you_type"),
            "originalTitle", Map.of("type", "search_as_you_type"),
            "embedding", Map.of("type", "knn_vector")));
  }

  private static Map<String, Object> textTitleMapping() {
    return Map.of(
        "properties",
        Map.of(
            "primaryTitle", Map.of("type", "text"),
            "originalTitle", Map.of("type", "text")));
  }
}
