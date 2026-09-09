package com.thecodinglab.imdbclone.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.catalog.api.MovieSearchReindexJobStatus;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchReindexJobs;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchReindexWorker;

/** Use the production rebuild protocol synchronously when preparing isolated search fixtures. */
public final class SearchIndexFixture {
  private SearchIndexFixture() {}

  public static void rebuild(MovieSearchReindexJobs jobs, MovieSearchReindexWorker worker) {
    var job = jobs.startReindex();
    for (int step = 0;
        step < 10000 && jobs.getStatus(job.jobId()).status() == MovieSearchReindexJobStatus.RUNNING;
        step++) {
      assertThat(worker.advance())
          .as("Rebuild fixture must make progress without storage failures")
          .isTrue();
    }
    assertThat(jobs.getStatus(job.jobId()).status())
        .isEqualTo(MovieSearchReindexJobStatus.COMPLETED);
  }
}
