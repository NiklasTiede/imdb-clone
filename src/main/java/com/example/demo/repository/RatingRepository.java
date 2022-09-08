package com.example.demo.repository;

import com.example.demo.entity.Account;
import com.example.demo.entity.Rating;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {

  List<Rating> findRatingsByMovieId(Long movieId);

  List<Rating> findRatingsByAccount(Account account);

  Optional<Rating> findRatingByAccountIdAndMovieId(Long accountId, Long movieId);

  List<Rating> findAllByModifiedAtInUtcAfter(Instant instant);
}
