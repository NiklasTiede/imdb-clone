package com.thecodinglab.imdbclone.identity.internal.security.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
public class SecurityAuditEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Instant occurredAt = Instant.now();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SecurityAuditEventType eventType;

  // Hibernate reads these fields directly when persisting audit events.
  @SuppressWarnings("UnusedVariable")
  private String principal;

  @SuppressWarnings("UnusedVariable")
  private Long accountId;

  @SuppressWarnings("UnusedVariable")
  private String ipAddress;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> details = Map.of();

  public SecurityAuditEvent() {}

  public SecurityAuditEvent(
      SecurityAuditEventType eventType, Long accountId, Map<String, Object> details) {
    this.eventType = eventType;
    this.accountId = accountId;
    this.details = details;
  }

  public SecurityAuditEvent(
      SecurityAuditEventType eventType,
      String principal,
      Long accountId,
      String ipAddress,
      Map<String, Object> details) {
    this.eventType = eventType;
    this.principal = principal;
    this.accountId = accountId;
    this.ipAddress = ipAddress;
    this.details = details;
  }
}
