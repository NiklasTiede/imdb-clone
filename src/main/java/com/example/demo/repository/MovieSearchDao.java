package com.example.demo.repository;

import com.example.demo.entity.Movie;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

/** LIKE queries with 9 mio Movie-table are not working with JpaRepository */
@Repository
public class MovieSearchDao {

  private final EntityManager entityManager;

  public MovieSearchDao(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public List<Movie> findByPrimaryTitleStartsWith(String startsWithPrimaryTitle) {
    TypedQuery<Movie> query =
        entityManager.createQuery(
            "SELECT m FROM Movie m WHERE m.primaryTitle LIKE :keyword", Movie.class);
    query.setParameter("keyword", startsWithPrimaryTitle + "%");
    query.setMaxResults(50);
    return query.getResultList();
  }
}
