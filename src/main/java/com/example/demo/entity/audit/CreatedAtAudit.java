package com.example.demo.entity.audit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;

@MappedSuperclass
@JsonIgnoreProperties(
    value = {"createdAtInUtc"},
    allowGetters = true)
public abstract class CreatedAtAudit {

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAtInUtc;

  public Instant getCreatedAtInUtc() {
    return createdAtInUtc;
  }
}
