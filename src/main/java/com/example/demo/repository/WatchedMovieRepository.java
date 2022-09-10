package com.example.demo.repository;

import com.example.demo.entity.WatchedMovie;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchedMovieRepository extends JpaRepository<WatchedMovie, Long> {

  Optional<WatchedMovie> findWatchedMovieByMovieIdAndAccountId(Long movieId, Long accountId);

  List<WatchedMovie> findAllByAccountIdOrderByCreatedAtInUtcDesc(Long accountId);
}
