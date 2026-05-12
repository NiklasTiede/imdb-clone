package com.thecodinglab.imdbclone.identity.internal.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

  Optional<VerificationToken> findByToken(String token);

  List<VerificationToken> findAllByExpiryDateInUtcBefore(Instant expiryDate);
}
