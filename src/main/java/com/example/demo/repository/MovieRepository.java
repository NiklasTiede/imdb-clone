package com.example.demo.repository;

import com.example.demo.entity.Movie;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieRepository extends JpaRepository<Movie, Integer> {

  Movie findByTitle(String title);

  List<Movie> findByTitleContaining(String title);

  // same query as above just witten by hand:
  @Query(value = "select u from Movie u where u.title like %:keyword%")
  List<Movie> findUsersByKeyword(@Param("keyword") String keyword);
}
