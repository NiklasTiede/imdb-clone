package com.thecodinglab.imdbclone.entity.audit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@MappedSuperclass
public abstract class CreatedAtAudit {

  @JsonIgnore
  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  @Field(
      type = FieldType.Date,
      format = {},
      pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXX")
  private Instant createdAtInUtc;

  public Instant getCreatedAtInUtc() {
    return createdAtInUtc;
  }
}
