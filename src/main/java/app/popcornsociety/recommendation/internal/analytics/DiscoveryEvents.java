package app.popcornsociety.recommendation.internal.analytics;

import app.popcornsociety.recommendation.api.DiscoveryEventRequest;
import app.popcornsociety.recommendation.api.DiscoveryEventService;
import app.popcornsociety.recommendation.api.DiscoveryEventSummary;
import app.popcornsociety.recommendation.api.DiscoveryEventType;
import app.popcornsociety.recommendation.internal.persistence.DiscoveryEvent;
import app.popcornsociety.recommendation.internal.persistence.DiscoveryEventRepository;
import app.popcornsociety.shared.error.BadRequestException;
import app.popcornsociety.shared.security.UserPrincipal;
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
