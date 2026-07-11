package com.thecodinglab.imdbclone.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class UserPrincipalTest {

  @Test
  void accountIdentityIsSharedWithSpecializedPrincipals() {
    UserPrincipal principal = principal(42L);
    UserPrincipal specializedPrincipal = new SpecializedUserPrincipal(42L);

    assertThat(principal).isEqualTo(specializedPrincipal);
    assertThat(specializedPrincipal).isEqualTo(principal).hasSameHashCodeAs(principal);
  }

  @Test
  void differentAccountIdsAreNotEqual() {
    assertThat(principal(42L)).isNotEqualTo(principal(43L));
  }

  private UserPrincipal principal(Long id) {
    return new UserPrincipal(
        id, "First", "Last", "user" + id, "user@example.com", "password", false, true, List.of());
  }

  private static final class SpecializedUserPrincipal extends UserPrincipal {

    private SpecializedUserPrincipal(Long id) {
      super(
          id, "First", "Last", "user" + id, "user@example.com", "password", false, true, List.of());
    }
  }
}
