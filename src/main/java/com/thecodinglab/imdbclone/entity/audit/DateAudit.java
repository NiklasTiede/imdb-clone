package com.thecodinglab.imdbclone.entity.audit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.hibernate.annotations.UpdateTimestamp;

@MappedSuperclass
public abstract class DateAudit extends CreatedAtAudit {

  @JsonIgnore
  @UpdateTimestamp
  @Column(nullable = false)
  private final Instant modifiedAtInUtc = Instant.now();

  public Instant getModifiedAtInUtc() {
    return modifiedAtInUtc;
  }
}
