package com.thecodinglab.imdbclone.catalog.internal.search.index;

import com.thecodinglab.imdbclone.catalog.api.MovieSearchReindexJobResponse;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchReindexJobStatus;
import com.thecodinglab.imdbclone.shared.error.NotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovieSearchReindexJobs {
  private final JdbcTemplate jdbc;

  public MovieSearchReindexJobs(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  public MovieSearchReindexJobResponse startReindex() {
    jdbc.queryForObject(
        "select pg_advisory_xact_lock(hashtextextended('imdb:catalog:reindex-start', 0))",
        Object.class);
    var running =
        jdbc.query("select * from movie_search_reindex_job where status = 'RUNNING'", this::map);
    if (!running.isEmpty()) {
      throw new MovieSearchReindexAlreadyRunningException(running.getFirst());
    }
    UUID id = UUID.randomUUID();
    jdbc.update(
        "insert into movie_search_reindex_job(id, total_movies) select ?, count(*) from movie", id);
    return getStatus(id);
  }

  @Transactional(readOnly = true)
  public MovieSearchReindexJobResponse getStatus(UUID jobId) {
    var jobs = jdbc.query("select * from movie_search_reindex_job where id = ?", this::map, jobId);
    if (jobs.isEmpty()) {
      throw new NotFoundException("Movie search reindex job [%s] was not found.".formatted(jobId));
    }
    return jobs.getFirst();
  }

  private MovieSearchReindexJobResponse map(ResultSet row, int index) throws SQLException {
    var finished = row.getTimestamp("finished_at");
    return new MovieSearchReindexJobResponse(
        row.getObject("id", UUID.class),
        MovieSearchReindexJobStatus.valueOf(row.getString("status")),
        row.getLong("indexed_movies"),
        row.getLong("total_movies"),
        row.getTimestamp("started_at").toInstant(),
        finished == null ? null : finished.toInstant(),
        row.getString("error_message"));
  }
}
