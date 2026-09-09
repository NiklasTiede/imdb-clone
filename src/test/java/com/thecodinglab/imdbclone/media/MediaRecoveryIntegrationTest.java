package com.thecodinglab.imdbclone.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.thecodinglab.imdbclone.account.api.AccountIdentityService;
import com.thecodinglab.imdbclone.account.api.AccountService;
import com.thecodinglab.imdbclone.catalog.api.MovieRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.catalog.internal.search.projection.MovieSearchProjectionTasks;
import com.thecodinglab.imdbclone.media.internal.MediaCleanup;
import com.thecodinglab.imdbclone.media.internal.MediaKind;
import com.thecodinglab.imdbclone.media.internal.MediaObjects;
import com.thecodinglab.imdbclone.media.internal.MediaRecovery;
import com.thecodinglab.imdbclone.media.internal.MediaService;
import com.thecodinglab.imdbclone.media.internal.MediaStorageProperties;
import com.thecodinglab.imdbclone.media.internal.MediaWork;
import com.thecodinglab.imdbclone.media.internal.images.Image;
import com.thecodinglab.imdbclone.shared.error.BadRequestException;
import com.thecodinglab.imdbclone.shared.error.NotFoundException;
import com.thecodinglab.imdbclone.shared.error.ObjectStorageOperationException;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import com.thecodinglab.imdbclone.support.BaseContainers;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

class MediaRecoveryIntegrationTest extends BaseContainers {
  @Autowired private MediaService media;
  @Autowired private MovieService movies;
  @Autowired private AccountIdentityService identities;
  @Autowired private com.thecodinglab.imdbclone.account.api.AccountImageService accountImages;
  @Autowired private AccountService accounts;
  @Autowired private MediaWork work;
  @Autowired private MediaCleanup cleanup;
  @Autowired private MediaRecovery recovery;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TransactionTemplate transactions;
  @Autowired private S3Client s3;
  @Autowired private MediaStorageProperties storage;
  @Autowired private MeterRegistry meters;
  @Autowired private DataSource dataSource;

  @Autowired
  @Qualifier("mediaRecoveryTask")
  private RecurringTask<Void> recoveryTask;

  @MockitoSpyBean private MediaObjects objects;
  @MockitoBean private MovieSearchProjectionTasks projection;
  private Long movieId;

  @BeforeEach
  void createIsolatedFixture() {
    jdbc.update("delete from media_object_work");
    movieId =
        movies
            .createMovie(
                new MovieRequest(
                    "Recovery test",
                    "Recovery test",
                    2020,
                    null,
                    90,
                    Set.of(),
                    MovieType.MOVIE,
                    false))
            .id();
  }

  @Test
  void partiallyUploadedObjectsAreRecoveredFromPersistedIntent() throws Exception {
    var forward = mockingDetails(objects).getMockCreationSettings().getDefaultAnswer();
    var writes = new AtomicInteger();
    var stored = new ConcurrentLinkedQueue<String>();
    doAnswer(
            invocation -> {
              if (writes.incrementAndGet() == 2) {
                throw storageFailure();
              }
              String result = (String) forward.answer(invocation);
              stored.add(invocation.<Image>getArgument(0).getImageName());
              return result;
            })
        .when(objects)
        .store(any(Image.class));
    var upload = upload();

    assertThatThrownBy(() -> media.storeMovieImage(upload, movieId))
        .isInstanceOf(ObjectStorageOperationException.class);

    assertThat(movies.getMovieImageToken(movieId).posterImageToken()).isNull();
    String token = pendingToken();
    assertThat(state(token)).isEqualTo("STAGED");
    assertThat(stored).hasSize(1);
    assertExists(stored.element());
    recoverOrphan(token);
    MediaKind.MOVIE.objectNames(token).forEach(this::assertMissing);
    assertPendingCount(0);
    recovery.recoverPending();
    assertPendingCount(0);
  }

  @Test
  void rolledBackAttachmentRetainsOldObjectsAndRecoversNewObjects() throws Exception {
    media.storeMovieImage(upload(), movieId);
    String previous = movies.getMovieImageToken(movieId).posterImageToken();
    var replacement = upload();
    transactions.executeWithoutResult(
        status -> {
          media.storeMovieImage(replacement, movieId);
          status.setRollbackOnly();
        });

    String orphan = pendingToken();
    assertThat(movies.getMovieImageToken(movieId).posterImageToken()).isEqualTo(previous);
    MediaKind.MOVIE.objectNames(previous).forEach(this::assertExists);
    MediaKind.MOVIE.objectNames(orphan).forEach(this::assertExists);
    recoverOrphan(orphan);
    MediaKind.MOVIE.objectNames(orphan).forEach(this::assertMissing);
    MediaKind.MOVIE.objectNames(previous).forEach(this::assertExists);
  }

  @Test
  void partialCleanupFailureIsPersistedObservedAndIdempotentlyRetried() throws Exception {
    media.storeMovieImage(upload(), movieId);
    String previous = movies.getMovieImageToken(movieId).posterImageToken();
    var names = MediaKind.MOVIE.objectNames(previous);
    var attempts = new AtomicInteger();
    var forward = mockingDetails(objects).getMockCreationSettings().getDefaultAnswer();
    double failures = meters.counter("media.cleanup.failures", "kind", "MOVIE").count();
    doAnswer(
            invocation -> {
              if (attempts.incrementAndGet() == 1) {
                s3.deleteObject(
                    request -> request.bucket(storage.bucketName()).key(names.getFirst()));
                throw storageFailure();
              }
              return forward.answer(invocation);
            })
        .when(objects)
        .delete(eq(MediaKind.MOVIE), eq(previous));

    media.storeMovieImage(upload(), movieId);

    recovery.recoverPending();
    String current = movies.getMovieImageToken(movieId).posterImageToken();
    assertThat(current).isNotEqualTo(previous);
    assertThat(state(previous)).isEqualTo("RETIRED");
    assertThat(
            jdbc.queryForObject(
                "select attempts from media_object_work where token = ?", Integer.class, previous))
        .isEqualTo(1);
    assertThat(meters.counter("media.cleanup.failures", "kind", "MOVIE").count())
        .isEqualTo(failures + 1);
    assertMissing(names.getFirst());
    assertExists(names.getLast());
    MediaKind.MOVIE.objectNames(current).forEach(this::assertExists);

    makeDue(previous);
    new MediaRecovery(jdbc, cleanup).recoverPending();
    cleanup.process(MediaKind.MOVIE, previous);
    names.forEach(this::assertMissing);
    MediaKind.MOVIE.objectNames(current).forEach(this::assertExists);
    assertPendingCount(0);
    assertThat(attempts.get()).isEqualTo(2);
  }

  @Test
  void recoverySkipsActiveUploadsAndFencesThemAfterAbandonment() throws Exception {
    String token = Image.generateToken();
    work.registerUpload(MediaKind.MOVIE, token);
    makeDue(token);
    var locked = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try (var executor = Executors.newSingleThreadExecutor()) {
      var upload =
          executor.submit(
              () ->
                  transactions.executeWithoutResult(
                      status -> {
                        work.lockUpload(MediaKind.MOVIE, token);
                        locked.countDown();
                        await(release);
                        status.setRollbackOnly();
                      }));
      try {
        await(locked);
        recovery.recoverPending();
        assertThat(state(token)).isEqualTo("STAGED");
      } finally {
        release.countDown();
      }
      upload.get(15, TimeUnit.SECONDS);
    }
    recovery.recoverPending();
    assertThat(state(token)).isEqualTo("RETIRED");
    assertThatThrownBy(
            () ->
                transactions.executeWithoutResult(
                    status -> work.lockUpload(MediaKind.MOVIE, token)))
        .isInstanceOf(BadRequestException.class);
    makeDue(token);
    recovery.recoverPending();
    assertPendingCount(0);
  }

  @Test
  void retirementDoesNotDeleteAnObjectStillReferencedByTheCatalog() throws Exception {
    media.storeMovieImage(upload(), movieId);
    String token = movies.getMovieImageToken(movieId).posterImageToken();
    transactions.executeWithoutResult(status -> work.retire(MediaKind.MOVIE, token));
    recovery.recoverPending();
    MediaKind.MOVIE.objectNames(token).forEach(this::assertExists);
    assertPendingCount(0);
  }

  @Test
  void simultaneousReplacementsRetainOnlyTheCommittedCurrentObjects() throws Exception {
    var forward = mockingDetails(objects).getMockCreationSettings().getDefaultAnswer();
    var stored = new ConcurrentLinkedQueue<String>();
    var bothUploaded = new CountDownLatch(2);
    doAnswer(
            invocation -> {
              var image = invocation.<Image>getArgument(0);
              String result = (String) forward.answer(invocation);
              stored.add(image.getImageName());
              if (image.getImageName().endsWith("_size_120x180.jpg")) {
                bothUploaded.countDown();
                await(bothUploaded);
              }
              return result;
            })
        .when(objects)
        .store(any(Image.class));
    var firstFile = upload();
    var secondFile = upload();
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> media.storeMovieImage(firstFile, movieId));
      var second = executor.submit(() -> media.storeMovieImage(secondFile, movieId));
      first.get(30, TimeUnit.SECONDS);
      second.get(30, TimeUnit.SECONDS);
    }
    recovery.recoverPending();
    var current =
        MediaKind.MOVIE.objectNames(movies.getMovieImageToken(movieId).posterImageToken());
    assertThat(stored).hasSize(4);
    for (String name : stored) {
      if (current.contains(name)) {
        assertExists(name);
      } else {
        assertMissing(name);
      }
    }
    assertPendingCount(0);
  }

  @Test
  void aBatchOfActiveUploadsCannotStarveUnrelatedCleanup() throws Exception {
    jdbc.update(
        """
        insert into media_object_work(kind, token, state, available_at)
        select 'MOVIE', 'active-' || number, 'STAGED', current_timestamp - interval '1 hour'
        from generate_series(1, 100) number
        """);
    String retired = "unrelated-retired";
    jdbc.update(
        """
        insert into media_object_work(kind, token, state, available_at)
        values ('MOVIE', ?, 'RETIRED', current_timestamp - interval '1 second')
        """,
        retired);
    var locked = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try (var executor = Executors.newSingleThreadExecutor()) {
      var active =
          executor.submit(
              () ->
                  transactions.executeWithoutResult(
                      status -> {
                        jdbc.queryForList(
                            "select token from media_object_work where state = 'STAGED' for update",
                            String.class);
                        locked.countDown();
                        await(release);
                      }));
      try {
        await(locked);
        recovery.recoverPending();
        assertThat(
                jdbc.queryForObject(
                    "select count(*) from media_object_work where token = ?",
                    Integer.class,
                    retired))
            .as("locked uploads must not fill the entire recovery batch")
            .isZero();
        assertPendingCount(100);
      } finally {
        release.countDown();
      }
      active.get(15, TimeUnit.SECONDS);
    }
  }

  @Test
  void deletionDuringUploadRejectsAttachmentAndLeavesRecoverableObjects() throws Exception {
    var forward = mockingDetails(objects).getMockCreationSettings().getDefaultAnswer();
    var uploaded = new CountDownLatch(1);
    var attach = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              String result = (String) forward.answer(invocation);
              if (invocation.<Image>getArgument(0).getImageName().endsWith("_size_120x180.jpg")) {
                uploaded.countDown();
                await(attach);
              }
              return result;
            })
        .when(objects)
        .store(any(Image.class));
    var file = upload();
    try (var executor = Executors.newSingleThreadExecutor()) {
      var uploading = executor.submit(() -> media.storeMovieImage(file, movieId));
      try {
        await(uploaded);
        movies.deleteMovie(movieId);
      } finally {
        attach.countDown();
      }
      assertThatThrownBy(() -> uploading.get(15, TimeUnit.SECONDS))
          .hasCauseInstanceOf(NotFoundException.class);
    }
    String orphan = pendingToken();
    MediaKind.MOVIE.objectNames(orphan).forEach(this::assertExists);
    recoverOrphan(orphan);
    MediaKind.MOVIE.objectNames(orphan).forEach(this::assertMissing);
    assertPendingCount(0);
  }

  @Test
  void registeredSchedulerRecoversFailedWorkAfterStopAndRestart() throws Exception {
    media.storeMovieImage(upload(), movieId);
    String token = movies.getMovieImageToken(movieId).posterImageToken();
    var attempts = new AtomicInteger();
    var forward = mockingDetails(objects).getMockCreationSettings().getDefaultAnswer();
    doAnswer(
            invocation -> {
              if (attempts.incrementAndGet() <= 2) {
                throw storageFailure();
              }
              return forward.answer(invocation);
            })
        .when(objects)
        .delete(MediaKind.MOVIE, token);
    media.deleteMovieImage(movieId);
    recovery.recoverPending(); // First durable worker attempt fails.
    makeDue(token);

    Scheduler first = newScheduler();
    try {
      first.scheduleIfNotExists(
          new com.github.kagkarlsson.scheduler.task.TaskInstance<Void>(
              "media-object-recovery", "recurring", null),
          Instant.now());
      first.reschedule(recoveryTask.getDefaultTaskInstance(), Instant.now());
      first.start();
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(15))
          .untilAsserted(
              () -> {
                assertThat(
                        jdbc.queryForObject(
                            "select attempts from media_object_work where token = ?",
                            Integer.class,
                            token))
                    .isEqualTo(2);
                assertThat(first.getCurrentlyExecuting()).isEmpty();
              });
    } finally {
      first.stop();
    }
    assertPendingCount(1);
    MediaKind.MOVIE.objectNames(token).forEach(this::assertExists);

    makeDue(token);
    Scheduler restarted = newScheduler();
    try {
      // Keep the registered task row across Scheduler instances; only advance the test deadline.
      restarted.reschedule(recoveryTask.getDefaultTaskInstance(), Instant.now());
      restarted.start();
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(15))
          .untilAsserted(
              () -> {
                assertPendingCount(0);
                assertThat(restarted.getCurrentlyExecuting()).isEmpty();
              });
    } finally {
      restarted.stop();
    }
    MediaKind.MOVIE.objectNames(token).forEach(this::assertMissing);
    assertThat(attempts.get()).isEqualTo(3);
  }

  @Test
  void accountDeletionDuringProfileUploadLeavesRecoverableOrphan() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    var account =
        identities.createAccountForIdentity(
            "media_" + suffix, suffix + "@example.com", "test-hash", true);
    var user =
        new UserPrincipal(
            account.id(),
            null,
            null,
            account.username(),
            account.email(),
            "test-hash",
            false,
            true,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
    var forward = mockingDetails(objects).getMockCreationSettings().getDefaultAnswer();
    var uploaded = new CountDownLatch(1);
    var attach = new CountDownLatch(1);
    var writes = new AtomicInteger();
    doAnswer(
            invocation -> {
              String result = (String) forward.answer(invocation);
              if (writes.incrementAndGet() == 2) {
                uploaded.countDown();
                await(attach);
              }
              return result;
            })
        .when(objects)
        .store(any(Image.class));
    var file =
        new MockMultipartFile(
            "image",
            "profile.jpeg",
            "image/jpeg",
            Files.readAllBytes(
                Path.of("src/main/resources/api-calls/object-storage/raw-profile-photo.jpeg")));
    try (var executor = Executors.newSingleThreadExecutor()) {
      var uploading = executor.submit(() -> media.storeProfilePhoto(file, user));
      try {
        await(uploaded);
        accounts.deleteAccount(account.username(), user);
      } finally {
        attach.countDown();
      }
      assertThatThrownBy(() -> uploading.get(15, TimeUnit.SECONDS))
          .hasCauseInstanceOf(NotFoundException.class);
    }
    String orphan = pendingToken();
    MediaKind.PROFILE.objectNames(orphan).forEach(this::assertExists);
    recoverOrphan(orphan);
    MediaKind.PROFILE.objectNames(orphan).forEach(this::assertMissing);
    assertPendingCount(0);
  }

  @Test
  void deletedTokenCannotBeAttachedAgainThroughMovieUpdate() throws Exception {
    media.storeMovieImage(upload(), movieId);
    String retired = movies.getMovieImageToken(movieId).posterImageToken();
    media.deleteMovieImage(movieId);
    recovery.recoverPending();
    MediaKind.MOVIE.objectNames(retired).forEach(this::assertMissing);
    assertThatThrownBy(() -> movies.updateMovie(movieId, requestWithToken(retired)))
        .isInstanceOf(BadRequestException.class);
    assertThat(movies.getMovieImageToken(movieId).posterImageToken()).isNull();
  }

  @Test
  void deletedTokenCannotBeAttachedToANewMovie() throws Exception {
    media.storeMovieImage(upload(), movieId);
    String retired = movies.getMovieImageToken(movieId).posterImageToken();
    media.deleteMovieImage(movieId);
    recovery.recoverPending();
    assertThatThrownBy(() -> movies.createMovie(requestWithToken(retired)))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void periodicRetirementAuditRemovesWritesArrivingAfterSuccessfulCleanup() throws Exception {
    media.storeMovieImage(upload(), movieId);
    String retired = movies.getMovieImageToken(movieId).posterImageToken();
    media.deleteMovieImage(movieId);
    recovery.recoverPending();
    String key = MediaKind.MOVIE.objectNames(retired).getFirst();
    assertMissing(key);
    for (int lateWrite = 0; lateWrite < 2; lateWrite++) {
      // Simulate an old PUT completing at the storage server after its client already timed out.
      s3.putObject(
          request -> request.bucket(storage.bucketName()).key(key),
          software.amazon.awssdk.core.sync.RequestBody.fromString("late remote completion"));
      assertExists(key);
      jdbc.update(
          "update media_retired_token set check_after = current_timestamp - interval '1 second' where token = ?",
          retired);
      new MediaRecovery(jdbc, cleanup).recoverPending();
      assertMissing(key);
      assertPendingCount(0);
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from media_retired_token where token = ?",
                  Integer.class,
                  retired))
          .isEqualTo(1);
    }
    assertThatThrownBy(() -> movies.updateMovie(movieId, requestWithToken(retired)))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void concurrentAttachmentWaitsForCleanupAndCannotResurrectItsToken() throws Exception {
    media.storeMovieImage(upload(), movieId);
    String retired = movies.getMovieImageToken(movieId).posterImageToken();
    var other = movies.createMovie(requestWithToken(null));
    var deleting = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var forward = mockingDetails(objects).getMockCreationSettings().getDefaultAnswer();
    doAnswer(
            invocation -> {
              deleting.countDown();
              await(release);
              return forward.answer(invocation);
            })
        .when(objects)
        .delete(MediaKind.MOVIE, retired);
    try (var pool = Executors.newFixedThreadPool(2)) {
      media.deleteMovieImage(movieId);
      var cleanupFuture = pool.submit(() -> recovery.recoverPending());
      try {
        await(deleting);
        var attachFuture =
            pool.submit(() -> movies.updateMovie(other.id(), requestWithToken(retired)));
        org.awaitility.Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(
                () ->
                    assertThat(
                            jdbc.queryForObject(
                                "select count(*) from pg_locks where locktype = 'advisory' and not granted",
                                Long.class))
                        .isPositive());
        assertThat(attachFuture.isDone()).isFalse();
        release.countDown();
        cleanupFuture.get(15, TimeUnit.SECONDS);
        assertThatThrownBy(() -> attachFuture.get(15, TimeUnit.SECONDS))
            .hasCauseInstanceOf(BadRequestException.class);
      } finally {
        release.countDown();
      }
    }
    assertThat(movies.getMovieImageToken(other.id()).posterImageToken()).isNull();
    MediaKind.MOVIE.objectNames(retired).forEach(this::assertMissing);
  }

  @Test
  void profileTokensAlsoStayRetired() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    var account =
        identities.createAccountForIdentity(
            "retired_" + suffix, suffix + "@example.com", "test-hash", true);
    String token = "profile-" + suffix;
    accountImages.updateProfileImageToken(account.id(), token);
    accountImages.clearProfileImageToken(account.id());
    recovery.recoverPending();
    assertThatThrownBy(() -> accountImages.updateProfileImageToken(account.id(), token))
        .isInstanceOf(BadRequestException.class);
  }

  private MovieRequest requestWithToken(String token) {
    return new MovieRequest(
        null,
        null,
        MovieType.MOVIE,
        "Token reuse",
        "Token reuse",
        false,
        2020,
        null,
        90,
        Set.of(),
        null,
        token,
        null,
        null);
  }

  private Scheduler newScheduler() {
    return Scheduler.create(dataSource)
        .startTasks(recoveryTask)
        .threads(1)
        .pollingInterval(Duration.ofMillis(100))
        .build();
  }

  private void recoverOrphan(String token) {
    makeDue(token);
    new MediaRecovery(jdbc, cleanup).recoverPending();
    assertThat(state(token)).isEqualTo("RETIRED");
    // The settlement interval prevents a delayed remote write from racing immediate deletion.
    recovery.recoverPending();
    assertPendingCount(1);
    makeDue(token);
    new MediaRecovery(jdbc, cleanup).recoverPending();
  }

  private void makeDue(String token) {
    jdbc.update(
        "update media_object_work set available_at = current_timestamp - interval '1 second' where token = ?",
        token);
  }

  private String pendingToken() {
    return jdbc.queryForObject("select token from media_object_work", String.class);
  }

  private String state(String token) {
    return jdbc.queryForObject(
        "select state from media_object_work where token = ?", String.class, token);
  }

  private void assertPendingCount(int count) {
    assertThat(jdbc.queryForObject("select count(*) from media_object_work", Integer.class))
        .isEqualTo(count);
  }

  private MockMultipartFile upload() throws Exception {
    return new MockMultipartFile(
        "image",
        "poster.jpg",
        "image/jpeg",
        Files.readAllBytes(
            Path.of("src/main/resources/api-calls/object-storage/raw-movie-image.jpg")));
  }

  private void assertExists(String key) {
    assertThat(
            s3.headObject(request -> request.bucket(storage.bucketName()).key(key)).contentLength())
        .isPositive();
  }

  private void assertMissing(String key) {
    assertThatThrownBy(
            () -> s3.headObject(request -> request.bucket(storage.bucketName()).key(key)))
        .isInstanceOfSatisfying(
            S3Exception.class, exception -> assertThat(exception.statusCode()).isEqualTo(404));
  }

  private static ObjectStorageOperationException storageFailure() {
    return new ObjectStorageOperationException(
        "Synthetic object storage outage", new IllegalStateException("injected"));
  }

  private static void await(CountDownLatch latch) {
    try {
      assertThat(latch.await(15, TimeUnit.SECONDS))
          .as("concurrent operation reached checkpoint")
          .isTrue();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError(exception);
    }
  }
}
