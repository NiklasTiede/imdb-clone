package com.example.demo.repository;

import com.example.demo.entity.Account;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.security.UserPrincipal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, Long> {

  Optional<Account> findByUsername(String username);

  Optional<Account> findByEmail(String email);

  Optional<Account> findByUsernameOrEmail(String username, String email);

  Boolean existsByUsername(String username);

  Boolean existsByEmail(String email);

  default Account getAccount(UserPrincipal currentUser) {
    return getAccountByName(currentUser.getUsername());
  }

  default Account getAccountByName(String username) {
    return findByUsername(username)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "User with username [" + username + "] not found in database."));
  }

  @Query("UPDATE Account a " + "SET a.enabled = TRUE WHERE a.email = ?1")
  int enableAppUser(String email);
}
