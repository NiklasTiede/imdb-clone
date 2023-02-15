package com.thecodinglab.imdbclone;

import com.thecodinglab.imdbclone.entity.Movie;
import com.thecodinglab.imdbclone.repository.*;
import com.thecodinglab.imdbclone.utility.PartitionList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CommandLineRunnerImpl implements CommandLineRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineRunnerImpl.class);

  private final MovieRepository movieRepository;
  private final MovieElasticSearchRepository movieSearchRepository;

  public CommandLineRunnerImpl(
      MovieRepository movieRepository, MovieElasticSearchRepository movieSearchRepository) {
    this.movieRepository = movieRepository;
    this.movieSearchRepository = movieSearchRepository;
  }

  @Override
  public void run(String... arg0) {

    // indexing a set of movies if no movies can be found in ES
    if (movieSearchRepository.count() < 100000) {
      List<Movie> popularMovies = movieRepository.findByImdbRatingCountBetween(1000, 200000);
      LOGGER.info("Number of popular movies to be indexed:  [{}]", popularMovies.size());

      List<List<Movie>> partitions = PartitionList.partition(popularMovies, 1000);
      partitions.forEach(movieSearchRepository::saveAll);
    }
  }
}
