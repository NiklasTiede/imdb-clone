package com.thecodinglab.imdbclone.catalog.internal.search;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieSearchProjectionTaskRepository
    extends JpaRepository<MovieSearchProjectionTask, Long> {

  List<MovieSearchProjectionTask> findTop100ByOrderByRequestedAtInUtcAsc();
}
