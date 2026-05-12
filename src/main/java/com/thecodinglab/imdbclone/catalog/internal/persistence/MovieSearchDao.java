package com.thecodinglab.imdbclone.catalog.internal.persistence;

import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import com.thecodinglab.imdbclone.shared.error.BadRequestException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.springframework.stereotype.Repository;

/** LIKE queries with 9 mio Movie-table are not working with JpaRepository */
@Repository
public class MovieSearchDao {

  private final EntityManager entityManager;

  MovieSearchDao(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public PagedResponse<Movie> findByPrimaryTitleStartsWith(
      String startsWithPrimaryTitle, int page, int size) {
    TypedQuery<Movie> query =
        entityManager.createQuery(
            "SELECT m FROM Movie m WHERE m.primaryTitle LIKE :keyword ORDER BY m.imdbRatingCount DESC",
            Movie.class);
    query.setParameter("keyword", startsWithPrimaryTitle + "%");
    List<Movie> result = query.getResultList();
    int totalElements = result.size();
    int totalPages = (int) Math.ceil((float) totalElements / size);
    if (page * size > totalElements) {
      throw new BadRequestException("Page number must be less than [" + totalPages + "].");
    }
    boolean isLast = (page + 1) * size > totalElements;
    if (size < totalElements) {
      result = result.subList(page * size, Math.min((page + 1) * size, totalElements));
    }
    return new PagedResponse<>(result, page, size, totalElements, totalPages, isLast);
  }
}
