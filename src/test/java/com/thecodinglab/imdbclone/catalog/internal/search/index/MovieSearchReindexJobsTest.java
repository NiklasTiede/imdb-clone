package com.thecodinglab.imdbclone.catalog.internal.search.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.api.MovieSearchReindexJobStatus;
import com.thecodinglab.imdbclone.shared.error.NotFoundException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovieSearchReindexJobsTest {

  @Mock private MovieSearchIndexMaintenance movieSearchIndexMaintenance;

  @Test
  void startReindexCreatesRunningJobAndTracksCompletion() {
    CapturingExecutor executor = new CapturingExecutor();
    MovieSearchReindexJobs jobs = new MovieSearchReindexJobs(movieSearchIndexMaintenance, executor);
    when(movieSearchIndexMaintenance.totalMovies()).thenReturn(12L);
    when(movieSearchIndexMaintenance.reindexMovies(any()))
        .thenAnswer(
            invocation -> {
              LongConsumer progress = invocation.getArgument(0);
              progress.accept(5L);
              progress.accept(12L);
              return 12L;
            });

    var startedJob = jobs.startReindex();

    assertThat(startedJob.status()).isEqualTo(MovieSearchReindexJobStatus.RUNNING);
    assertThat(startedJob.indexedMovies()).isZero();
    assertThat(startedJob.totalMovies()).isEqualTo(12L);

    executor.runNext();

    var completedJob = jobs.getStatus(startedJob.jobId());
    assertThat(completedJob.status()).isEqualTo(MovieSearchReindexJobStatus.COMPLETED);
    assertThat(completedJob.indexedMovies()).isEqualTo(12L);
    assertThat(completedJob.errorMessage()).isNull();
    verify(movieSearchIndexMaintenance).reindexMovies(any());
  }

  @Test
  void startReindexRejectsConcurrentJobs() {
    CapturingExecutor executor = new CapturingExecutor();
    MovieSearchReindexJobs jobs = new MovieSearchReindexJobs(movieSearchIndexMaintenance, executor);
    when(movieSearchIndexMaintenance.totalMovies()).thenReturn(12L);

    jobs.startReindex();

    assertThatThrownBy(jobs::startReindex)
        .isInstanceOf(MovieSearchReindexAlreadyRunningException.class);
  }

  @Test
  void failedReindexHasACompleteFailureState() {
    CapturingExecutor executor = new CapturingExecutor();
    MovieSearchReindexJobs jobs = new MovieSearchReindexJobs(movieSearchIndexMaintenance, executor);
    when(movieSearchIndexMaintenance.totalMovies()).thenReturn(12L);
    when(movieSearchIndexMaintenance.reindexMovies(any()))
        .thenThrow(new IllegalStateException("index unavailable"));

    var startedJob = jobs.startReindex();
    executor.runNext();

    var failedJob = jobs.getStatus(startedJob.jobId());
    assertThat(failedJob.status()).isEqualTo(MovieSearchReindexJobStatus.FAILED);
    assertThat(failedJob.finishedAt()).isNotNull();
    assertThat(failedJob.errorMessage()).isEqualTo("index unavailable");
  }

  @Test
  void getStatusRejectsUnknownJobIds() {
    CapturingExecutor executor = new CapturingExecutor();
    MovieSearchReindexJobs jobs = new MovieSearchReindexJobs(movieSearchIndexMaintenance, executor);

    assertThatThrownBy(() -> jobs.getStatus(UUID.randomUUID()))
        .isInstanceOf(NotFoundException.class);
  }

  private static final class CapturingExecutor implements Executor {
    private final Queue<Runnable> tasks = new ArrayDeque<>();

    @Override
    public void execute(Runnable command) {
      tasks.add(command);
    }

    void runNext() {
      tasks.remove().run();
    }
  }
}
