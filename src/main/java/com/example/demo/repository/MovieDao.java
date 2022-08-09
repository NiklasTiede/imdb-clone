package com.example.demo.repository;

import com.example.demo.entity.Movie;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;

@Repository
public class MovieDao {

  private final EntityManager entityManager;

  public MovieDao(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public List<Movie> findSomeMovies(String startsWithPrimaryTitle) {
    Query query =
        this.entityManager.createNativeQuery(
            "SELECT m.* FROM IMDBCLONE.movie m WHERE m.primary_title LIKE :keyword");
    //    query.setParameter("title" + "%", title);
    query.setParameter("keyword", startsWithPrimaryTitle + "%");
    return query.getResultList();
  }
}
