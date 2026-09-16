package app.popcornsociety.engagement.api;

import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("recommendation")
public interface WatchlistCandidateProvider {

  List<WatchlistCandidate> findCandidates(Long accountId);
}
