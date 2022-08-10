package com.example.demo.repository;

import com.example.demo.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

  Account findByUsername(String username);

  Account findByEmail(String email);

  void deleteAccountByEmail(String username);
}
