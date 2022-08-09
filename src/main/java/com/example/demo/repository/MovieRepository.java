package com.example.demo.repository;

import com.example.demo.entity.Movie;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieRepository extends JpaRepository<Movie, Long> {

  List<Movie> findByPrimaryTitleContaining(String title);

  List<Movie> findByPrimaryTitleStartsWith(String primaryTitle);

  @Query(
      "SELECT m from Movie m where m.primaryTitle LIKE 'the CoNJuring%' ORDER BY m.imdbRatingCount DESC")
  List<Movie> findMoviesBla();

  @Query("SELECT m FROM Movie m WHERE m.primaryTitle LIKE :keyword%")
  List<Movie> searchByTitleLike(@Param("keyword") String keyword);

  @Query(
      value = "SELECT * FROM Movie m WHERE m.primary_title LIKE 'The Conjuring%'",
      nativeQuery = true)
  List<Movie> searchByTitleLike2();

  //  @Query(value = "select u from Movie u where u.primaryTitle like :keyword%")
  //  List<Movie> findMoviesByKeyword2(@Param("keyword") String keyword);
  //
  //
  //  @Query(value = "select u from Movie u where u.primaryTitle" + " like %:keyword%")
  //  List<Movie> findMoviesByKeyword(@Param("keyword") String keyword);

  // page, sort

  // custom queries, where suited
}
