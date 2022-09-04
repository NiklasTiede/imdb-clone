package com.example.demo.entity.audit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import org.hibernate.annotations.UpdateTimestamp;

@MappedSuperclass
@JsonIgnoreProperties(
    value = {"modifiedAtInUtc"},
    allowGetters = true)
public abstract class DateAudit extends CreatedAtAudit {

  @UpdateTimestamp
  @Column(nullable = false)
  private Instant modifiedAtInUtc = Instant.now();

  public Instant getModifiedAtInUtc() {
    return modifiedAtInUtc;
  }
}
