package com.thecodinglab.imdbclone.identity.internal.security.audit;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityAuditEventRepository extends JpaRepository<SecurityAuditEvent, Long> {

  long deleteByOccurredAtBefore(Instant cutoff);
}
