package com.thecodinglab.imdbclone.account.internal.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalCredentialRepository extends JpaRepository<LocalCredential, Long> {

  Optional<LocalCredential> findByAccountId(Long accountId);

  boolean existsByAccountId(Long accountId);
}
