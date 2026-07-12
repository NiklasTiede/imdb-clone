package com.thecodinglab.imdbclone.recommendation.internal.persistence;

import com.thecodinglab.imdbclone.recommendation.api.DiscoveryEventType;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscoveryEventRepository extends JpaRepository<DiscoveryEvent, Long> {
  boolean existsByEventId(String eventId);

  long countByEventTypeAndCreatedAtInUtcGreaterThanEqual(
      DiscoveryEventType eventType, Instant since);

  long countByCreatedAtInUtcGreaterThanEqual(Instant since);
}
