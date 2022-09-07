package com.example.demo.repository;

import com.example.demo.entity.WatchedMovie;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchedMovieRepository extends JpaRepository<WatchedMovie, Long> {

  List<WatchedMovie> findAllByMovieId(Long movieId);

  List<WatchedMovie> findAllByAccountId(Long accountId);

  List<WatchedMovie> findAllByAccountIdOrderByCreatedAtInUtcDesc(Long accountId);
}
