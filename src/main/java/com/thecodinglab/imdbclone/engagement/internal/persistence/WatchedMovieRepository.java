package com.thecodinglab.imdbclone.engagement.internal.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchedMovieRepository extends JpaRepository<WatchedMovie, WatchedMovieId> {

  Optional<WatchedMovie> findByIdMovieIdAndIdAccountId(Long movieId, Long accountId);

  Page<WatchedMovie> findAllByIdAccountIdOrderByCreatedAtInUtcDesc(
      Long accountId, Pageable pageable);

  Long countByIdAccountId(Long accountId);
}
