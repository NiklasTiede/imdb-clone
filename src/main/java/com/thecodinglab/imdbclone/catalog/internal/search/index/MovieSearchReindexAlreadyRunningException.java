package com.thecodinglab.imdbclone.catalog.internal.search.index;

import com.thecodinglab.imdbclone.catalog.api.MovieSearchReindexJobResponse;

public class MovieSearchReindexAlreadyRunningException extends RuntimeException {

  private final MovieSearchReindexJobResponse runningJob;

  public MovieSearchReindexAlreadyRunningException(MovieSearchReindexJobResponse runningJob) {
    super("Movie search reindex job is already running.");
    this.runningJob = runningJob;
  }

  public MovieSearchReindexJobResponse runningJob() {
    return runningJob;
  }
}
