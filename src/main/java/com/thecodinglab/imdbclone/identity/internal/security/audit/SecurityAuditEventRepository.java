package com.thecodinglab.imdbclone.identity.internal.security.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityAuditEventRepository extends JpaRepository<SecurityAuditEvent, Long> {}
