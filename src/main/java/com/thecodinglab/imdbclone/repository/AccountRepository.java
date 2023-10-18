package com.thecodinglab.imdbclone.repository;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.exception.domain.NotFoundException;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

  Optional<Account> findByUsername(String username);

  Optional<Account> findByEmail(String email);

  Optional<Account> findByUsernameOrEmail(String username, String email);

  Boolean existsByUsername(String username);

  Boolean existsByEmail(String email);

  default Account getAccount(UserPrincipal currentUser) {
    return getAccountByUsername(currentUser.getUsername());
  }

  default Account getAccountByUsername(String username) {
    return findByUsername(username)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "User with username [" + username + "] not found in database."));
  }
}
