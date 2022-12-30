package com.thecodinglab.imdbclone.entity.audit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
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
