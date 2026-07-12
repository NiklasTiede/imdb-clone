package com.thecodinglab.imdbclone.recommendation.internal.analytics;

import com.thecodinglab.imdbclone.recommendation.api.DiscoveryEventRequest;
import com.thecodinglab.imdbclone.recommendation.api.DiscoveryEventService;
import com.thecodinglab.imdbclone.recommendation.api.DiscoveryEventSummary;
import com.thecodinglab.imdbclone.recommendation.api.DiscoveryEventType;
import com.thecodinglab.imdbclone.recommendation.internal.persistence.DiscoveryEvent;
import com.thecodinglab.imdbclone.recommendation.internal.persistence.DiscoveryEventRepository;
import com.thecodinglab.imdbclone.shared.error.BadRequestException;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DiscoveryEvents implements DiscoveryEventService {

  private final DiscoveryEventRepository repository;

  DiscoveryEvents(DiscoveryEventRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void record(DiscoveryEventRequest request, UserPrincipal currentAccount) {
    if (request.eventType().requiresMovie() && request.movieId() == null) {
      throw new BadRequestException("movieId is required for " + request.eventType() + " events.");
    }
    if (repository.existsByEventId(request.eventId())) {
      return;
    }

    repository.save(
        new DiscoveryEvent(
            request.eventId(),
            request.eventType(),
            sha256(request.sessionId()),
            sha256(request.feedInstanceId()),
            request.sectionId(),
            request.position(),
            request.movieId(),
            currentAccount == null ? null : currentAccount.getId(),
            request.strategyVersion()));
  }

  @Override
  @Transactional(readOnly = true)
  public DiscoveryEventSummary summary(int days) {
    Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
    Map<DiscoveryEventType, Long> eventCounts =
        Arrays.stream(DiscoveryEventType.values())
            .collect(
                Collectors.toUnmodifiableMap(
                    type -> type,
                    type ->
                        repository.countByEventTypeAndCreatedAtInUtcGreaterThanEqual(type, since)));
    return new DiscoveryEventSummary(
        days, repository.countByCreatedAtInUtcGreaterThanEqual(since), eventCounts);
  }

  private String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 must be available in the JVM.", exception);
    }
  }
}
