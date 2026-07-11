package com.thecodinglab.imdbclone.catalog.internal.search.index;

import com.thecodinglab.imdbclone.catalog.api.MovieSearchReindexJobResponse;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchReindexJobStatus;
import com.thecodinglab.imdbclone.shared.error.NotFoundException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class MovieSearchReindexJobs {

  private final MovieSearchIndexMaintenance movieSearchIndexMaintenance;
  private final Executor executor;
  private final AtomicReference<UUID> activeJobId = new AtomicReference<>();
  private final ConcurrentMap<UUID, ReindexJob> jobs = new ConcurrentHashMap<>();

  public MovieSearchReindexJobs(
      MovieSearchIndexMaintenance movieSearchIndexMaintenance,
      @Qualifier("movieSearchReindexExecutor") Executor executor) {
    this.movieSearchIndexMaintenance = movieSearchIndexMaintenance;
    this.executor = executor;
  }

  public MovieSearchReindexJobResponse startReindex() {
    UUID jobId = UUID.randomUUID();
    ReindexJob job =
        new ReindexJob(jobId, movieSearchIndexMaintenance.totalMovies(), Instant.now());
    UUID runningJobId = activeJobId.get();
    if (!activeJobId.compareAndSet(null, jobId)) {
      throw new MovieSearchReindexAlreadyRunningException(getStatus(runningJobId));
    }

    jobs.put(jobId, job);
    try {
      executor.execute(() -> runReindex(job));
    } catch (RuntimeException ex) {
      activeJobId.compareAndSet(jobId, null);
      job.fail(ex);
      throw ex;
    }
    return job.toResponse();
  }

  public MovieSearchReindexJobResponse getStatus(UUID jobId) {
    ReindexJob job = jobs.get(jobId);
    if (job == null) {
      throw new NotFoundException("Movie search reindex job [%s] was not found.".formatted(jobId));
    }
    return job.toResponse();
  }

  private void runReindex(ReindexJob job) {
    try {
      long indexedMovies = movieSearchIndexMaintenance.reindexMovies(job::updateProgress);
      job.complete(indexedMovies);
    } catch (RuntimeException ex) {
      job.fail(ex);
    } finally {
      activeJobId.compareAndSet(job.id(), null);
    }
  }

  private static final class ReindexJob {
    private final UUID id;
    private final long totalMovies;
    private final Instant startedAt;
    private ReindexJobState state = new Running(0);

    private ReindexJob(UUID id, long totalMovies, Instant startedAt) {
      this.id = id;
      this.totalMovies = totalMovies;
      this.startedAt = startedAt;
    }

    UUID id() {
      return id;
    }

    synchronized void updateProgress(long indexedMovies) {
      if (!(state instanceof Running)) {
        throw new IllegalStateException("Cannot update a finished reindex job");
      }
      state = new Running(indexedMovies);
    }

    synchronized void complete(long indexedMovies) {
      state = new Completed(indexedMovies, Instant.now());
    }

    synchronized void fail(RuntimeException ex) {
      state = new Failed(state.indexedMovies(), Instant.now(), errorMessage(ex));
    }

    synchronized MovieSearchReindexJobResponse toResponse() {
      return switch (state) {
        case Running running ->
            response(MovieSearchReindexJobStatus.RUNNING, running.indexedMovies(), null, null);
        case Completed completed ->
            response(
                MovieSearchReindexJobStatus.COMPLETED,
                completed.indexedMovies(),
                completed.finishedAt(),
                null);
        case Failed failed ->
            response(
                MovieSearchReindexJobStatus.FAILED,
                failed.indexedMovies(),
                failed.finishedAt(),
                failed.errorMessage());
      };
    }

    private MovieSearchReindexJobResponse response(
        MovieSearchReindexJobStatus status,
        long indexedMovies,
        Instant finishedAt,
        String errorMessage) {
      return new MovieSearchReindexJobResponse(
          id, status, indexedMovies, totalMovies, startedAt, finishedAt, errorMessage);
    }

    private static String errorMessage(RuntimeException exception) {
      return exception.getMessage() != null
          ? exception.getMessage()
          : exception.getClass().getSimpleName();
    }
  }

  private sealed interface ReindexJobState permits Running, Completed, Failed {
    long indexedMovies();
  }

  private record Running(long indexedMovies) implements ReindexJobState {}

  private record Completed(long indexedMovies, Instant finishedAt) implements ReindexJobState {}

  private record Failed(long indexedMovies, Instant finishedAt, String errorMessage)
      implements ReindexJobState {}
}
