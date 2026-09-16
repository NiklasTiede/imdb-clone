package app.popcornsociety.recommendation.api;

import app.popcornsociety.shared.security.UserPrincipal;

public interface DiscoveryEventService {

  void record(DiscoveryEventRequest request, UserPrincipal currentAccount);

  DiscoveryEventSummary summary(int days);
}
