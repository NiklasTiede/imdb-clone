package com.thecodinglab.imdbclone.identity.internal;

import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationToken;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VerificationTokenCleanupScheduler {

  private static final Logger logger =
      LoggerFactory.getLogger(VerificationTokenCleanupScheduler.class);

  private final VerificationTokenRepository verificationTokenRepository;

  public VerificationTokenCleanupScheduler(
      VerificationTokenRepository verificationTokenRepository) {
    this.verificationTokenRepository = verificationTokenRepository;
  }

  /** Cleaning job deleting 30-day old tokens. The job is scheduled at 1:10 AM */
  @Scheduled(cron = "0 10 1 * * *")
  public void deleteExpiredVerificationTokens() {
    List<VerificationToken> oldExpiredVerificationTokens =
        verificationTokenRepository.findAllByExpiryDateInUtcBefore(
            Instant.now().minus(30, ChronoUnit.DAYS));
    Integer oldTokensCount = oldExpiredVerificationTokens.size();
    verificationTokenRepository.deleteAll(oldExpiredVerificationTokens);
    logger.info(
        "[{}] expired verification tokens older than 4 weeks were deleted ", oldTokensCount);
  }
}
