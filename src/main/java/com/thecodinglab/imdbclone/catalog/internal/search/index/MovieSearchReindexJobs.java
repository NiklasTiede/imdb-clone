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
    private MovieSearchReindexJobStatus status = MovieSearchReindexJobStatus.RUNNING;
    private long indexedMovies;
    private Instant finishedAt;
    private String errorMessage;

    private ReindexJob(UUID id, long totalMovies, Instant startedAt) {
      this.id = id;
      this.totalMovies = totalMovies;
      this.startedAt = startedAt;
    }

    UUID id() {
      return id;
    }

    synchronized void updateProgress(long indexedMovies) {
      this.indexedMovies = indexedMovies;
    }

    synchronized void complete(long indexedMovies) {
      this.indexedMovies = indexedMovies;
      this.status = MovieSearchReindexJobStatus.COMPLETED;
      this.finishedAt = Instant.now();
    }

    synchronized void fail(RuntimeException ex) {
      this.status = MovieSearchReindexJobStatus.FAILED;
      this.finishedAt = Instant.now();
      this.errorMessage = ex.getMessage();
    }

    synchronized MovieSearchReindexJobResponse toResponse() {
      return new MovieSearchReindexJobResponse(
          id, status, indexedMovies, totalMovies, startedAt, finishedAt, errorMessage);
    }
  }
}
