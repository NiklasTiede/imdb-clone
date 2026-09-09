package com.thecodinglab.imdbclone.engagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thecodinglab.imdbclone.account.api.AccountIdentityService;
import com.thecodinglab.imdbclone.account.api.AccountService;
import com.thecodinglab.imdbclone.account.internal.persistence.AccountRepository;
import com.thecodinglab.imdbclone.catalog.api.MovieImageService;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import com.thecodinglab.imdbclone.engagement.api.RatingScore;
import com.thecodinglab.imdbclone.engagement.api.RatingService;
import com.thecodinglab.imdbclone.shared.error.NotFoundException;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import com.thecodinglab.imdbclone.support.BaseContainers;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {"db-scheduler.enabled=true", "db-scheduler.polling-interval=1h"})
class RatingConsistencyIntegrationTest extends BaseContainers {

  @Autowired private RatingService ratings;
  @Autowired private AccountService accounts;
  @Autowired private AccountIdentityService identities;
  @Autowired private AccountRepository accountRepository;
  @Autowired private MovieRepository movies;
  @Autowired private MovieImageService movieImages;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PlatformTransactionManager transactions;

  private UserPrincipal user;
  private final List<Long> movieIds = new ArrayList<>();
  private final List<Long> additionalAccounts = new ArrayList<>();

  @BeforeEach
  void createFixtures() {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    var account =
        identities.createAccountForIdentity(
            "rating_" + suffix, suffix + "@example.com", "synthetic-password-hash", true);
    user =
        new UserPrincipal(
            account.id(),
            null,
            null,
            account.username(),
            account.email(),
            null,
            false,
            true,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
    movieIds.add(movies.save(new Movie("First", "First", MovieType.MOVIE, 90)).getId());
    movieIds.add(movies.save(new Movie("Second", "Second", MovieType.MOVIE, 90)).getId());
  }

  @AfterEach
  void cleanupFixtures() {
    for (Long movieId : movieIds) {
      jdbc.update(
          "delete from scheduled_tasks where task_name = 'movie-search-projection' and task_instance = ?",
          movieId.toString());
      movies.deleteById(movieId);
    }
    accountRepository.deleteById(user.getId());
    additionalAccounts.forEach(accountRepository::deleteById);
  }

  @Test
  void movieImageUpdateCannotOverwriteAConcurrentRatingAggregate() throws Exception {
    Long movieId = movieIds.getFirst();
    overlap(
        () -> movieImages.updateMovieImageToken(movieId, "new-poster"), () -> rate(movieId, "7.0"));

    assertThat(movieImages.getMovieImageToken(movieId).posterImageToken()).isEqualTo("new-poster");
    assertAggregate(movieId, 1, "7.0");
  }

  @Test
  void accountDeletionRemovesRatingsAndUpdatesEveryAffectedAggregateAndProjection() {
    rate(movieIds.get(0), "7.0");
    rate(movieIds.get(1), "9.0");
    clearProjectionTasks();

    accounts.deleteAccount(user.getUsername(), user);

    for (Long movieId : movieIds) {
      assertAggregate(movieId, 0, "0.0");
      assertThat(projectionCount(movieId)).isOne();
    }
  }

  @Test
  void deletingOneAccountPreservesOtherAccountsRatingContributions() {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    var other =
        identities.createAccountForIdentity(
            "other_" + suffix, suffix + "@example.com", "synthetic-password-hash", true);
    additionalAccounts.add(other.id());
    var otherUser =
        new UserPrincipal(
            other.id(),
            null,
            null,
            other.username(),
            other.email(),
            null,
            false,
            true,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
    Long movieId = movieIds.getFirst();
    ratings.rateMovie(otherUser, movieId, RatingScore.of(new BigDecimal("3.0")));
    rate(movieId, "7.0");

    accounts.deleteAccount(user.getUsername(), user);

    assertAggregate(movieId, 1, "3.0");
  }

  @Test
  void concurrentDeletesRemoveTheContributionOnlyOnce() throws Exception {
    Long movieId = movieIds.getFirst();
    rate(movieId, "7.0");
    overlap(
        () -> ratings.deleteRating(user, movieId),
        () ->
            assertThatThrownBy(() -> ratings.deleteRating(user, movieId))
                .isInstanceOf(NotFoundException.class));
    assertAggregate(movieId, 0, "0.0");
  }

  @Test
  void concurrentAccountDeletesKeepTheNotFoundContractAndRemoveContributionOnlyOnce()
      throws Exception {
    Long movieId = movieIds.getFirst();
    rate(movieId, "7.0");
    overlap(
        () -> accounts.deleteAccount(user.getUsername(), user),
        () ->
            assertThatThrownBy(() -> accounts.deleteAccount(user.getUsername(), user))
                .isInstanceOf(NotFoundException.class));
    assertAggregate(movieId, 0, "0.0");
  }

  @Test
  void concurrentFirstRatingsAreSerializedAsCreateThenUpdate() throws Exception {
    Long movieId = movieIds.getFirst();
    overlap(() -> rate(movieId, "7.0"), () -> rate(movieId, "9.0"));
    assertAggregate(movieId, 1, "9.0");
  }

  @Test
  void concurrentUpdatesCalculateDeltaFromTheCommittedPreviousScore() throws Exception {
    Long movieId = movieIds.getFirst();
    rate(movieId, "5.0");
    overlap(() -> rate(movieId, "7.0"), () -> rate(movieId, "9.0"));
    assertAggregate(movieId, 1, "9.0");
  }

  @Test
  void ratingAfterConcurrentDeletionBecomesANewRating() throws Exception {
    Long movieId = movieIds.getFirst();
    rate(movieId, "5.0");
    overlap(() -> ratings.deleteRating(user, movieId), () -> rate(movieId, "9.0"));
    assertAggregate(movieId, 1, "9.0");
  }

  @Test
  void accountDeletionWaitsForInFlightRatingAndRemovesItsContribution() throws Exception {
    Long movieId = movieIds.getFirst();
    overlap(() -> rate(movieId, "7.0"), () -> accounts.deleteAccount(user.getUsername(), user));
    assertAggregate(movieId, 0, "0.0");
    assertThat(accountRepository.findById(user.getId())).isEmpty();
  }

  @Test
  void rollbackRestoresRatingAggregateAndDoesNotLeaveProjectionWork() {
    Long movieId = movieIds.getFirst();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              rate(movieId, "7.0");
              status.setRollbackOnly();
            });
    assertAggregate(movieId, 0, "0.0");
    assertThat(projectionCount(movieId)).isZero();
  }

  @Test
  void rolledBackAccountDeletionRestoresRatingsAndAggregate() {
    Long movieId = movieIds.getFirst();
    rate(movieId, "7.0");
    clearProjectionTasks();
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              accounts.deleteAccount(user.getUsername(), user);
              status.setRollbackOnly();
            });
    assertAggregate(movieId, 1, "7.0");
    assertThat(accountRepository.findById(user.getId())).isPresent();
    assertThat(projectionCount(movieId)).isZero();
  }

  private void rate(Long movieId, String score) {
    ratings.rateMovie(user, movieId, RatingScore.of(new BigDecimal(score)));
  }

  private void assertAggregate(Long movieId, int count, String sum) {
    Movie movie = movies.getMovieById(movieId);
    assertThat(movie.getRatingCount()).isEqualTo(count);
    assertThat(movie.getRatingSum()).isEqualByComparingTo(sum);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from rating where movie_id = ?", Integer.class, movieId))
        .isEqualTo(count);
    assertThat(
            jdbc.queryForObject(
                "select coalesce(sum(rating), 0) from rating where movie_id = ?",
                BigDecimal.class,
                movieId))
        .isEqualByComparingTo(movie.getRatingSum());
    if (count == 0) {
      assertThat(movie.getRating()).isNull();
    } else {
      assertThat(movie.getRating())
          .isEqualByComparingTo(
              new BigDecimal(sum)
                  .divide(BigDecimal.valueOf(count), 1, java.math.RoundingMode.HALF_UP));
    }
  }

  private int projectionCount(Long movieId) {
    return jdbc.queryForObject(
        "select count(*) from scheduled_tasks where task_name = 'movie-search-projection' and task_instance = ?",
        Integer.class,
        movieId.toString());
  }

  private void clearProjectionTasks() {
    for (Long movieId : movieIds) {
      jdbc.update(
          "delete from scheduled_tasks where task_name = 'movie-search-projection' and task_instance = ?",
          movieId.toString());
    }
  }

  private void overlap(Runnable firstOperation, Runnable secondOperation) throws Exception {
    CountDownLatch firstChanged = new CountDownLatch(1);
    CountDownLatch allowCommit = new CountDownLatch(1);
    CountDownLatch secondStarted = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(
              () ->
                  new TransactionTemplate(transactions)
                      .executeWithoutResult(
                          status -> {
                            firstOperation.run();
                            firstChanged.countDown();
                            await(allowCommit);
                          }));
      try {
        assertThat(firstChanged.await(10, TimeUnit.SECONDS)).isTrue();
        var second =
            executor.submit(
                () -> {
                  secondStarted.countDown();
                  secondOperation.run();
                });
        assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS))
            .isInstanceOf(TimeoutException.class);
        allowCommit.countDown();
        first.get(10, TimeUnit.SECONDS);
        second.get(10, TimeUnit.SECONDS);
      } finally {
        allowCommit.countDown();
      }
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(15, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for transaction release");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }
}
