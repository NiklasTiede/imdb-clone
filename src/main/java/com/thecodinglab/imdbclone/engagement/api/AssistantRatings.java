package com.thecodinglab.imdbclone.engagement.api;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public interface AssistantRatings {
  AssistantActionReceipt rate(Long accountId, Long movieId, BigDecimal score, UUID operationId);

  AssistantActionReceipt remove(Long accountId, Long movieId, UUID operationId);
}
