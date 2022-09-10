package com.example.demo.repository;

import com.example.demo.entity.Movie;
import com.example.demo.exceptions.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {

  default Movie getMovieById(Long movieId) {
    return findById(movieId)
        .orElseThrow(
            () -> new NotFoundException("Movie with id [" + movieId + "] not found in database."));
  }
}
