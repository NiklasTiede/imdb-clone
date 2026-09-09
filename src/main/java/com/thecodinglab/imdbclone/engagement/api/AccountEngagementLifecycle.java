package com.thecodinglab.imdbclone.engagement.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("lifecycle")
public interface AccountEngagementLifecycle {

  /**
   * Removes the account's rating contributions before account deletion, in the caller's
   * transaction. Serializes with rating mutations, updates catalog aggregates and schedules their
   * projections. Other relations referencing the account are removed by the database's existing
   * foreign-key cascades.
   */
  void removeAccountRatings(Long accountId);
}
