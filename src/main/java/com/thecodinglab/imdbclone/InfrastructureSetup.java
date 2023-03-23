package com.thecodinglab.imdbclone;

import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.repository.MovieElasticSearchRepository;
import com.thecodinglab.imdbclone.repository.MovieRepository;
import com.thecodinglab.imdbclone.service.ElasticSearchService;
import com.thecodinglab.imdbclone.service.FileStorageService;
import com.thecodinglab.imdbclone.utility.PartitionList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

@Component
public class InfrastructureSetup implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(InfrastructureSetup.class);

  private final MovieRepository movieRepository;
  private final MovieElasticSearchRepository movieSearchRepository;
  private final ElasticSearchService elasticSearchService;
  private final ElasticsearchOperations elasticsearchOperations;
  private final FileStorageService fileStorageService;

  @Value("${minio.rest.bucketName}")
  public String bucketName;

  public InfrastructureSetup(
      MovieRepository movieRepository,
      MovieElasticSearchRepository movieSearchRepository,
      ElasticSearchService elasticSearchService,
      ElasticsearchOperations elasticsearchOperations,
      FileStorageService fileStorageService) {
    this.movieRepository = movieRepository;
    this.movieSearchRepository = movieSearchRepository;
    this.elasticSearchService = elasticSearchService;
    this.elasticsearchOperations = elasticsearchOperations;
    this.fileStorageService = fileStorageService;
  }

  @Override
  public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {

    // indexing a set of movies if no movies can be found in Elasticsearch
    if (elasticsearchOperations.indexOps(IndexCoordinates.of("movies")).exists()
        && movieSearchRepository.count() < 100) {
      List<Movie> popularMovies = movieRepository.findByImdbRatingCountBetween(20000, 20000000);
      LOGGER.info("Number of popular movies to be indexed:  [{}]", popularMovies.size());
      List<List<Movie>> partitions = PartitionList.partition(popularMovies, 1000);
      partitions.forEach(elasticSearchService::indexMovies);
    }

    // create bucket/directories and set bucket policy in minIO if not existent
    fileStorageService.setUpBucket();

    LOGGER.info("Application is ready: MinIO bucket created and elasticsearch Data loaded");
  }
}
