package com.thecodinglab.imdbclone.catalog.api;

import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("recommendation")
public interface MovieDiscoveryCandidateProvider {

  List<MovieRecord> findCandidates(MovieDiscoveryCriteria criteria, int candidateLimit);
}
