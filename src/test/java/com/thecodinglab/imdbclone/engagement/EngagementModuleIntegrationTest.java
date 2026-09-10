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
  @Autowired private com.thecodinglab.imdbclone.engagement.api.AssistantRatings assistantRatings;

  @Autowired
  private com.thecodinglab.imdbclone.engagement.api.AssistantWatchlist assistantWatchlist;

  @Autowired private com.thecodinglab.imdbclone.engagement.api.WatchedMovieService watchlist;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ApplicationContext context;
  @MockitoBean private MovieReferenceService movies;
  @MockitoBean private MovieRatingAggregateService aggregates;

  @Autowired
  private com.thecodinglab.imdbclone.engagement.api.AssistantRatingLibrary assistantRatingLibrary;

  @Autowired
  private com.thecodinglab.imdbclone.engagement.api.RatingPreferenceProvider ratingPreferences;

  @Test
  void personalReadModelsUseOwnScoresAndExcludeAllSavedOrRatedMovies() {
    var first = org.mockito.Mockito.mock(com.thecodinglab.imdbclone.catalog.api.MovieRecord.class);
    var second = org.mockito.Mockito.mock(com.thecodinglab.imdbclone.catalog.api.MovieRecord.class);
    org.mockito.Mockito.when(first.id()).thenReturn(1L);
    org.mockito.Mockito.when(first.primaryTitle()).thenReturn("Forrest Gump");
    org.mockito.Mockito.when(second.id()).thenReturn(2L);
    org.mockito.Mockito.when(second.primaryTitle()).thenReturn("Arrival");
    org.mockito.Mockito.when(
            movies.findMoviesByIds(org.mockito.ArgumentMatchers.<Long>anyCollection()))
        .thenAnswer(
            invocation -> {
              java.util.Collection<Long> ids = invocation.getArgument(0);
              return java.util.stream.Stream.of(first, second)
                  .filter(m -> ids.contains(m.id()))
                  .toList();
            });
    assistantRatings.rate(2L, 1L, new BigDecimal("9.0"), java.util.UUID.randomUUID());
    assistantRatings.rate(2L, 2L, new BigDecimal("6.0"), java.util.UUID.randomUUID());
    assistantRatings.rate(1L, 2L, new BigDecimal("10.0"), java.util.UUID.randomUUID());
    assistantWatchlist.add(2L, 2L, java.util.UUID.randomUUID());
    var highest =
        assistantRatingLibrary.read(
            2L, 0, com.thecodinglab.imdbclone.engagement.api.AssistantRatingLibrary.Order.HIGHEST);
    assertThat(highest.items().getContent())
        .extracting(r -> r.movie().id())
        .containsExactly(1L, 2L);
    assertThat(highest.items().getContent())
        .extracting(r -> r.userScore())
        .containsExactly(new BigDecimal("9.0"), new BigDecimal("6.0"));
    assertThat(highest.averageUserScore()).isEqualByComparingTo("7.5");
    assertThat(
            assistantRatingLibrary
                .read(
                    2L,
                    0,
                    com.thecodinglab.imdbclone.engagement.api.AssistantRatingLibrary.Order.LOWEST)
                .items()
                .getContent())
        .extracting(r -> r.movie().id())
        .containsExactly(2L, 1L);
    assertThat(
            assistantRatingLibrary
                .read(
                    2L,
                    1,
                    com.thecodinglab.imdbclone.engagement.api.AssistantRatingLibrary.Order.HIGHEST)
                .items()
                .getContent())
        .isEmpty();
    var taste = ratingPreferences.forAccount(2L);
    assertThat(taste.totalRatings()).isEqualTo(2);
    assertThat(taste.favorites()).extracting(f -> f.movieId()).containsExactly(1L);
    assertThat(taste.excludedMovieIds()).containsExactlyInAnyOrder(1L, 2L);
    assistantRatings.remove(2L, 2L, java.util.UUID.randomUUID());
    assertThat(ratingPreferences.forAccount(2L).excludedMovieIds()).contains(2L);
    assertThat(ratingPreferences.forAccount(2L).totalRatings()).isEqualTo(1);
  }

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

  @Test
  void concurrentAddsCommitOneEntryAndReplayTheSameDurableReceipt() throws Exception {
    jdbc.update("delete from watched_movie where account_id = 2 and movie_id = 1");
    // The mapper only forwards the record; persistence validates the real movie FK.
    org.mockito.Mockito.when(movies.findMovieById(1L)).thenReturn(null);
    var operation = java.util.UUID.randomUUID();
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(6)) {
      var futures =
          java.util.stream.IntStream.range(0, 6)
              .mapToObj(i -> pool.submit(() -> assistantWatchlist.add(2L, 1L, operation)))
              .toList();
      var first = futures.getFirst().get();
      for (var future : futures) assertThat(future.get()).isEqualTo(first);
      assertThat(first.created()).isTrue();
    }
    assertThat(
            jdbc.queryForObject(
                "select count(*) from watched_movie where account_id = 2 and movie_id = 1",
                Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from engagement_action_receipt where account_id = 2 and operation_id = ?",
                Integer.class,
                operation))
        .isEqualTo(1);
    assertThat(assistantWatchlist.add(2L, 1L, java.util.UUID.randomUUID()).created()).isFalse();
    assertThatThrownBy(() -> assistantWatchlist.add(2L, 2L, operation))
        .isInstanceOf(IllegalArgumentException.class);
    watchlist.deleteWatchedMovie(1L, user());
    assistantWatchlist.add(2L, 1L, operation);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from watched_movie where account_id = 2 and movie_id = 1",
                Integer.class))
        .isZero();
  }

  @Test
  void failedCatalogLookupDoesNotPersistAnActionReceipt() {
    var operation = java.util.UUID.randomUUID();
    org.mockito.Mockito.when(movies.findMovieById(1L))
        .thenThrow(new IllegalStateException("synthetic"));
    assertThatThrownBy(() -> assistantWatchlist.add(2L, 1L, operation))
        .isInstanceOf(IllegalStateException.class);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from engagement_action_receipt where operation_id = ?",
                Integer.class,
                operation))
        .isZero();
  }

  @Test
  void ratingCreateUpdateRemoveReplayAndAccountIsolation() throws Exception {
    jdbc.update("delete from rating where account_id = 2 and movie_id = 1");
    var create = java.util.UUID.randomUUID();
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(6)) {
      var futures =
          java.util.stream.IntStream.range(0, 6)
              .mapToObj(
                  i ->
                      pool.submit(
                          () -> assistantRatings.rate(2L, 1L, new BigDecimal("8.5"), create)))
              .toList();
      var first = futures.getFirst().get();
      for (var future : futures) assertThat(future.get()).isEqualTo(first);
      assertThat(first.changed()).isTrue();
      assertThat(first.previousScore()).isNull();
    }
    verify(aggregates).applyRatingAggregateDelta(1L, new BigDecimal("8.5"), 1);
    var update = assistantRatings.rate(2L, 1L, new BigDecimal("9"), java.util.UUID.randomUUID());
    assertThat(update.previousScore()).isEqualByComparingTo("8.5");
    verify(aggregates).applyRatingAggregateDelta(1L, new BigDecimal("0.5"), 0);
    assertThat(
            assistantRatings
                .rate(2L, 1L, new BigDecimal("9.0"), java.util.UUID.randomUUID())
                .changed())
        .isFalse();
    assertThatThrownBy(() -> assistantRatings.rate(2L, 1L, new BigDecimal("7"), create))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> assistantWatchlist.remove(2L, 1L, create))
        .isInstanceOf(IllegalArgumentException.class);
    var removal = java.util.UUID.randomUUID();
    var removed = assistantRatings.remove(2L, 1L, removal);
    assertThat(removed.previousScore()).isEqualByComparingTo("9");
    verify(aggregates).applyRatingAggregateDelta(1L, new BigDecimal("-9.0"), -1);
    assertThat(assistantRatings.remove(2L, 1L, removal)).isEqualTo(removed);
    assertThat(assistantRatings.remove(2L, 1L, java.util.UUID.randomUUID()).changed()).isFalse();
    assistantRatings.rate(2L, 1L, new BigDecimal("8.5"), create);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from rating where account_id = 2 and movie_id = 1", Integer.class))
        .isZero();
    assistantRatings.rate(2L, 1L, new BigDecimal("6"), java.util.UUID.randomUUID());
    assistantRatings.remove(2L, 1L, removal);
    assertThat(
            jdbc.queryForObject(
                "select rating from rating where account_id = 2 and movie_id = 1",
                BigDecimal.class))
        .isEqualByComparingTo("6");
    // The same operation ID belongs to a different account and cannot replay another user's
    // receipt.
    var other = assistantRatings.rate(1L, 1L, new BigDecimal("7"), create);
    assertThat(other.score()).isEqualByComparingTo("7");
    assertThat(
            jdbc.queryForObject(
                "select rating from rating where account_id = 2 and movie_id = 1",
                BigDecimal.class))
        .isEqualByComparingTo("6");
  }

  @Test
  void watchlistRemovalIsIdempotentAndAnOldRemovalCannotDeleteALaterAddition() {
    assistantWatchlist.add(2L, 1L, java.util.UUID.randomUUID());
    var operation = java.util.UUID.randomUUID();
    var removed = assistantWatchlist.remove(2L, 1L, operation);
    assertThat(removed.changed()).isTrue();
    assertThat(assistantWatchlist.remove(2L, 1L, operation)).isEqualTo(removed);
    assertThat(assistantWatchlist.remove(2L, 1L, java.util.UUID.randomUUID()).changed()).isFalse();
    assistantWatchlist.add(2L, 1L, java.util.UUID.randomUUID());
    assistantWatchlist.remove(2L, 1L, operation);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from watched_movie where account_id = 2 and movie_id = 1",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void invalidScoresAndAggregateFailuresNeverCommitAReceiptOrPartialRating() {
    jdbc.update("delete from rating where account_id = 2 and movie_id = 1");
    for (String score : List.of("-1", "10.1", "8.55")) {
      assertThatThrownBy(
              () ->
                  assistantRatings.rate(2L, 1L, new BigDecimal(score), java.util.UUID.randomUUID()))
          .isInstanceOf(com.thecodinglab.imdbclone.shared.error.BadRequestException.class);
    }
    var operation = java.util.UUID.randomUUID();
    doThrow(new IllegalStateException("Synthetic Catalog failure"))
        .when(aggregates)
        .applyRatingAggregateDelta(1L, new BigDecimal("7.0"), 1);
    assertThatThrownBy(() -> assistantRatings.rate(2L, 1L, new BigDecimal("7"), operation))
        .isInstanceOf(IllegalStateException.class);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from engagement_action_receipt where operation_id = ?",
                Integer.class,
                operation))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from rating where account_id = 2 and movie_id = 1", Integer.class))
        .isZero();
  }

  @Autowired
  private com.thecodinglab.imdbclone.engagement.internal.AssistantActionReceipts receipts;

  @Autowired private org.springframework.transaction.PlatformTransactionManager transactions;

  @Test
  void assistantReadsOnlyTheRequestedAccountsWatchlistWithFixedPageSize() {
    jdbc.update("delete from watched_movie where account_id = 2");
    assistantWatchlist.add(2L, 1L, java.util.UUID.randomUUID());
    var movie = org.mockito.Mockito.mock(com.thecodinglab.imdbclone.catalog.api.MovieRecord.class);
    org.mockito.Mockito.when(movie.id()).thenReturn(1L);
    org.mockito.Mockito.when(movies.findMoviesByIds(List.of(1L))).thenReturn(List.of(movie));
    var page = assistantWatchlist.read(2L, 0);
    assertThat(page.getSize()).isEqualTo(20);
    assertThat(page.getPage()).isZero();
    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.isLast()).isTrue();
    assertThat(page.getContent())
        .singleElement()
        .satisfies(
            entry -> {
              assertThat(entry.accountId()).isEqualTo(2L);
              assertThat(entry.movieId()).isEqualTo(1L);
              assertThat(entry.movie().id()).isEqualTo(1L);
            });
    assertThat(assistantWatchlist.read(2L, 1).getContent()).isEmpty();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"watchlist_remove", "rating_set"})
  void receiptReplayRejectsAddingOrDroppingTheOriginalScore(String kind) {
    var operation = java.util.UUID.randomUUID();
    boolean rating = kind.equals("rating_set");
    var original =
        rating
            ? assistantRatings.rate(2L, 1L, new BigDecimal("8.5"), operation)
            : assistantWatchlist.remove(2L, 1L, operation);
    BigDecimal alteredScore = rating ? null : new BigDecimal("8.5");
    var transaction = new org.springframework.transaction.support.TransactionTemplate(transactions);
    assertThatThrownBy(
            () ->
                transaction.executeWithoutResult(
                    status ->
                        receipts.execute(
                            2L,
                            1L,
                            operation,
                            kind,
                            alteredScore,
                            () -> {
                              throw new AssertionError(
                                  "Conflicting replay must not execute a mutation");
                            })))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Operation already used for another command");
    var replay =
        rating
            ? assistantRatings.rate(2L, 1L, new BigDecimal("8.50"), operation)
            : assistantWatchlist.remove(2L, 1L, operation);
    assertThat(replay).isEqualTo(original);
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
