package com.example.demo.repository;

import com.example.demo.entity.Account;
import com.example.demo.entity.WatchedMovie;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchedMovieRepository extends JpaRepository<WatchedMovie, Long> {

  Optional<WatchedMovie> findWatchedMovieByMovieIdAndAccountId(Long movieId, Long accountId);

  Page<WatchedMovie> findAllByAccountIdOrderByCreatedAtInUtcDesc(Long accountId, Pageable pageable);

  Long countWatchedMoviesByAccount(Account account);
}
