package com.thecodinglab.imdbclone.engagement.api;

import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("recommendation")
public interface WatchlistCandidateProvider {

  List<WatchlistCandidate> findCandidates(Long accountId);
}
