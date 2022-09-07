package com.example.demo.entity.audit;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;

@MappedSuperclass
public abstract class CreatedAtAudit {

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAtInUtc;

  public Instant getCreatedAtInUtc() {
    return createdAtInUtc;
  }
}
