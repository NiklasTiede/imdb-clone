package com.example.demo.repository;

import com.example.demo.entity.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

  Optional<Account> findByUsername(String username);

  Optional<Account> findByEmail(String email);

  Optional<Account> findByUsernameOrEmail(String email, String username);

  Boolean existsByUsername(String username);

  Boolean existsByEmail(String email);
}
