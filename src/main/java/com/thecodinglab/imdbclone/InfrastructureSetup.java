package com.thecodinglab.imdbclone;

import static com.thecodinglab.imdbclone.utility.Log.COUNT;
import static net.logstash.logback.argument.StructuredArguments.v;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.google.common.collect.Lists;
import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.repository.MovieElasticSearchRepository;
import com.thecodinglab.imdbclone.repository.MovieRepository;
import com.thecodinglab.imdbclone.service.FileStorageService;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

@Component
public class InfrastructureSetup implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger logger = LoggerFactory.getLogger(InfrastructureSetup.class);

  private final MovieRepository movieRepository;
  private final MovieElasticSearchRepository movieSearchRepository;
  private final ElasticsearchOperations elasticsearchOperations;
  private final ElasticsearchClient elasticsearchClient;
  private final FileStorageService fileStorageService;

  @Value("${minio.rest.bucket-name}")
  public String bucketName;

  public InfrastructureSetup(
      MovieRepository movieRepository,
      MovieElasticSearchRepository movieSearchRepository,
      ElasticsearchOperations elasticsearchOperations,
      ElasticsearchClient elasticsearchClient,
      FileStorageService fileStorageService) {
    this.movieRepository = movieRepository;
    this.movieSearchRepository = movieSearchRepository;
    this.elasticsearchOperations = elasticsearchOperations;
    this.elasticsearchClient = elasticsearchClient;
    this.fileStorageService = fileStorageService;
  }

  @Override
  public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
    createMoviesIndexIfMissing();

    // indexing a set of movies if no movies can be found in Elasticsearch
    if (movieSearchRepository.count() < 100) {
      List<Movie> popularMovies = movieRepository.findByImdbRatingCountBetween(20000, 20000000);
      logger.info("Number of popular movies to be indexed: [{}]", v(COUNT, popularMovies.size()));
      List<List<Movie>> partitions = Lists.partition(popularMovies, 1000);
      partitions.forEach(movieSearchRepository::saveAll);
    }

    // create bucket/directories and set bucket policy in minIO if not existent
    fileStorageService.setUpBucket();

    logger.info("Application is ready: MinIO bucket created and elasticsearch Data loaded");
  }

  private void createMoviesIndexIfMissing() {
    try {
      elasticsearchClient.indices().get(index -> index.index("movies"));
    } catch (ElasticsearchException ex) {
      if (ex.status() != 404) {
        throw ex;
      }
      elasticsearchOperations.indexOps(Movie.class).createWithMapping();
    } catch (java.io.IOException ex) {
      throw new IllegalStateException("Failed to check Elasticsearch movies index", ex);
    }
  }
}
