package com.thecodinglab.imdbclone.catalog.internal.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieElasticSearchRepository;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

@ExtendWith(MockitoExtension.class)
class MovieSearchIndexMaintenanceTest {

  @Mock private MovieRepository movieRepository;
  @Mock private MovieElasticSearchRepository movieSearchRepository;
  @Mock private ElasticsearchOperations elasticsearchOperations;
  @Mock private IndexOperations indexOperations;

  private MovieSearchIndexMaintenance maintenance;

  @BeforeEach
  void setUp() {
    maintenance =
        new MovieSearchIndexMaintenance(
            movieRepository, movieSearchRepository, elasticsearchOperations);
  }

  @Test
  void reindexMoviesCreatesTheIndexWhenMissingAndIndexesEveryMovie() {
    Movie firstMovie = new Movie("First", "First", null, 90);
    Movie secondMovie = new Movie("Second", "Second", null, 95);
    PageRequest firstPage = PageRequest.of(0, 500, Sort.by("id").ascending());
    PageRequest secondPage = PageRequest.of(1, 500, Sort.by("id").ascending());

    when(elasticsearchOperations.indexOps(Movie.class)).thenReturn(indexOperations);
    when(indexOperations.exists()).thenReturn(false);
    when(movieRepository.findAll(firstPage))
        .thenReturn(new PageImpl<>(List.of(firstMovie), firstPage, 501));
    when(movieRepository.findAll(secondPage))
        .thenReturn(new PageImpl<>(List.of(secondMovie), secondPage, 501));

    long indexedMovies = maintenance.reindexMovies();

    assertThat(indexedMovies).isEqualTo(2);
    verify(indexOperations).createWithMapping();
    verify(movieSearchRepository).deleteAll();
    verify(movieSearchRepository).saveAll(List.of(firstMovie));
    verify(movieSearchRepository).saveAll(List.of(secondMovie));
  }

  @Test
  void reindexMoviesKeepsExistingIndexMapping() {
    PageRequest firstPage = PageRequest.of(0, 500, Sort.by("id").ascending());

    when(elasticsearchOperations.indexOps(Movie.class)).thenReturn(indexOperations);
    when(indexOperations.exists()).thenReturn(true);
    when(movieRepository.findAll(firstPage)).thenReturn(new PageImpl<>(List.of(), firstPage, 0));

    long indexedMovies = maintenance.reindexMovies();

    assertThat(indexedMovies).isZero();
    verify(indexOperations, never()).createWithMapping();
  }
}
