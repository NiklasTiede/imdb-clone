package com.thecodinglab.imdbclone.entity.audit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@MappedSuperclass
public abstract class DateAudit extends CreatedAtAudit {

  @JsonIgnore
  @UpdateTimestamp
  @Column(nullable = false)
  @Field(
      type = FieldType.Date,
      format = {},
      pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXX")
  private final Instant modifiedAtInUtc = Instant.now();

  public Instant getModifiedAtInUtc() {
    return modifiedAtInUtc;
  }
}
