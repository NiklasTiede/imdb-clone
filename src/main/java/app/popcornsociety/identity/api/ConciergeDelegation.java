package app.popcornsociety.identity.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public interface ConciergeDelegation {
  Actor verify(String token, String requiredScope);

  record Actor(Long accountId, String sessionBinding) {}
}
