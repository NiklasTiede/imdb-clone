package com.thecodinglab.imdbclone.repository;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.entity.Rating;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {

  List<Rating> findRatingsByMovieId(Long movieId);

  Page<Rating> findRatingsByAccount(Account account, Pageable pageable);

  Optional<Rating> findRatingByAccountIdAndMovieId(Long accountId, Long movieId);

  List<Rating> findAllByModifiedAtInUtcAfter(Instant instant);

  Long countRatingsByAccount(Account account);
}
