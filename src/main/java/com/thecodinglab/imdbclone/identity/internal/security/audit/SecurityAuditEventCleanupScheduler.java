package com.thecodinglab.imdbclone.identity.internal.security.audit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditEventCleanupScheduler {

  private static final Logger logger =
      LoggerFactory.getLogger(SecurityAuditEventCleanupScheduler.class);

  private final SecurityAuditEvents auditEvents;

  public SecurityAuditEventCleanupScheduler(SecurityAuditEvents auditEvents) {
    this.auditEvents = auditEvents;
  }

  /** Cleaning job deleting 90-day old security audit events. The job is scheduled at 1:20 AM. */
  @Scheduled(cron = "0 20 1 * * *")
  public void deleteOldSecurityAuditEvents() {
    long deletedEvents =
        auditEvents.deleteEventsOlderThan(Instant.now().minus(90, ChronoUnit.DAYS));
    logger.info("[{}] security audit events older than 90 days were deleted", deletedEvents);
  }
}
