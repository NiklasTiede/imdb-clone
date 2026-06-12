package com.thecodinglab.imdbclone.identity.internal.security.audit;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityAuditEvents {

  private final SecurityAuditEventRepository repository;

  public SecurityAuditEvents(SecurityAuditEventRepository repository) {
    this.repository = repository;
  }

  public void recordCredentialEvent(
      SecurityAuditEventType type, Long accountId, Map<String, Object> details) {
    repository.save(new SecurityAuditEvent(type, accountId, details));
  }

  public void recordAuthenticationEvent(
      SecurityAuditEventType type,
      String principal,
      Long accountId,
      String ipAddress,
      Map<String, Object> details) {
    repository.save(new SecurityAuditEvent(type, principal, accountId, ipAddress, details));
  }

  @Transactional
  public long deleteEventsOlderThan(java.time.Instant cutoff) {
    return repository.deleteByOccurredAtBefore(cutoff);
  }
}
