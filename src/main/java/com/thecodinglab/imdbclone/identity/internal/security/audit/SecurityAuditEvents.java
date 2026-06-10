package com.thecodinglab.imdbclone.identity.internal.security.audit;

import java.util.Map;
import org.springframework.stereotype.Service;

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
}
