package com.thecodinglab.imdbclone.engagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.thecodinglab.imdbclone.catalog.api.MovieRatingAggregateService;
import com.thecodinglab.imdbclone.catalog.api.MovieReferenceService;
import com.thecodinglab.imdbclone.engagement.api.RatingScore;
import com.thecodinglab.imdbclone.engagement.api.RatingService;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import com.thecodinglab.imdbclone.support.ModulePostgresSupport;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

@ApplicationModuleTest(
    webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE)
@Sql(scripts = "/sql/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EngagementModuleIntegrationTest extends ModulePostgresSupport {
  @Autowired private RatingService ratings;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ApplicationContext context;
  @MockitoBean private MovieReferenceService movies;
  @MockitoBean private MovieRatingAggregateService aggregates;

  @Test
  void ownsRatingPersistenceWhileCallingOnlyTheCatalogAggregateContract() {
    assertThat(context.getBeanNamesForType(org.springframework.data.repository.Repository.class))
        .containsExactlyInAnyOrder(
            "ratingRepository", "commentRepository", "watchedMovieRepository");
    assertThat(
            context
                .getBean(jakarta.persistence.EntityManagerFactory.class)
                .getMetamodel()
                .getEntities())
        .extracting(entity -> entity.getJavaType().getSimpleName())
        .containsExactlyInAnyOrder("Rating", "Comment", "WatchedMovie");
    assertThat(
            context.getBeanNamesForType(com.thecodinglab.imdbclone.catalog.api.MovieService.class))
        .isEmpty();
    assertThat(
            context.getBeanNamesForType(
                com.thecodinglab.imdbclone.account.api.AccountService.class))
        .isEmpty();
    jdbc.update("delete from rating where account_id = 2 and movie_id = 1");
    ratings.rateMovie(user(), 1L, new RatingScore(new BigDecimal("7.0")));
    assertThat(
            jdbc.queryForObject(
                "select rating from rating where account_id = 2 and movie_id = 1",
                BigDecimal.class))
        .isEqualByComparingTo("7.0");
    verify(aggregates).applyRatingAggregateDelta(1L, new BigDecimal("7.0"), 1);
  }

  @Test
  void failureOfTheAggregateContractRollsBackTheLocalRating() {
    jdbc.update("delete from rating where account_id = 2 and movie_id = 1");
    doThrow(new IllegalStateException("Synthetic Catalog failure"))
        .when(aggregates)
        .applyRatingAggregateDelta(1L, new BigDecimal("7.0"), 1);
    assertThatThrownBy(() -> ratings.rateMovie(user(), 1L, new RatingScore(new BigDecimal("7.0"))))
        .isInstanceOf(IllegalStateException.class);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from rating where account_id = 2 and movie_id = 1", Integer.class))
        .isZero();
  }

  private UserPrincipal user() {
    return new UserPrincipal(
        2L,
        null,
        null,
        "test_user_two",
        "two@web.com",
        "test-hash",
        false,
        true,
        List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }
}
