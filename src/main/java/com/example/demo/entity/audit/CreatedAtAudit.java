package com.example.demo.entity.audit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;

@MappedSuperclass
public abstract class CreatedAtAudit {

  @JsonIgnore
  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAtInUtc;

  public Instant getCreatedAtInUtc() {
    return createdAtInUtc;
  }
}
