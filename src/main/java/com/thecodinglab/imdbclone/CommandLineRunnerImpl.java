package com.thecodinglab.imdbclone;

import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.repository.*;
import com.thecodinglab.imdbclone.repository.MovieElasticSearchRepository;
import com.thecodinglab.imdbclone.service.ElasticSearchService;
import com.thecodinglab.imdbclone.utility.PartitionList;
import jakarta.transaction.Transactional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

@Component
public class CommandLineRunnerImpl implements CommandLineRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineRunnerImpl.class);

  private final MovieRepository movieRepository;
  private final MovieElasticSearchRepository movieSearchRepository;
  private final ElasticSearchService elasticSearchService;

  private final ElasticsearchOperations elasticsearchOperations;

  public CommandLineRunnerImpl(
      MovieRepository movieRepository,
      MovieElasticSearchRepository movieSearchRepository,
      ElasticSearchService elasticSearchService,
      ElasticsearchOperations elasticsearchOperations) {
    this.movieRepository = movieRepository;
    this.movieSearchRepository = movieSearchRepository;
    this.elasticSearchService = elasticSearchService;
    this.elasticsearchOperations = elasticsearchOperations;
  }

  @Transactional
  @Override
  public void run(String... arg0) {

    // indexing a set of movies if no movies can be found in ES
    if (elasticsearchOperations.indexOps(IndexCoordinates.of("movies")).exists()
        && movieSearchRepository.count() < 100000) {
      List<Movie> popularMovies = movieRepository.findByImdbRatingCountBetween(100, 20000000);
      LOGGER.info("Number of popular movies to be indexed:  [{}]", popularMovies.size());
      List<List<Movie>> partitions = PartitionList.partition(popularMovies, 1000);
      partitions.forEach(elasticSearchService::indexMovies);
    }
  }
}
