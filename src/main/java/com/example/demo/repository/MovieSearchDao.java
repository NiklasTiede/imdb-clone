package com.example.demo.repository;

import com.example.demo.entity.Movie;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;

/** LIKE queries with 9 mio Movie-table are not working with JpaRepository */
@Repository
public class MovieSearchDao {

  private final EntityManager entityManager;

  public MovieSearchDao(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public List<Movie> findByPrimaryTitleStartsWith(String startsWithPrimaryTitle) {
    Query query =
        this.entityManager.createNativeQuery(
            "SELECT m.* FROM IMDBCLONE.movie m WHERE m.primary_title LIKE :keyword");
    query.setParameter("keyword", startsWithPrimaryTitle + "%");
    return query.getResultList();
  }
}
