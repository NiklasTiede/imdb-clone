package com.example.demo.repository;

import com.example.demo.entity.Account;
import com.example.demo.entity.Movie;
import com.example.demo.entity.Rating;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {

  List<Rating> findRatingsByMovie(Movie movie);

  List<Rating> findRatingsByAccount(Account account);
}
