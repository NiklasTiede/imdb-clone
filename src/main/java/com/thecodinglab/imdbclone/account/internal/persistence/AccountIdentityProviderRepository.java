package com.thecodinglab.imdbclone.account.internal.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountIdentityProviderRepository
    extends JpaRepository<AccountIdentityProvider, Long> {

  Optional<AccountIdentityProvider> findByProviderAndProviderUserId(
      String provider, String providerUserId);
}
