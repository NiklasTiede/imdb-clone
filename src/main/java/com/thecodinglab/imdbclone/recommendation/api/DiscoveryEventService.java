package com.thecodinglab.imdbclone.recommendation.api;

import com.thecodinglab.imdbclone.shared.security.UserPrincipal;

public interface DiscoveryEventService {

  void record(DiscoveryEventRequest request, UserPrincipal currentAccount);

  DiscoveryEventSummary summary(int days);
}
