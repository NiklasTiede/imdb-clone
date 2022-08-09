package com.example.demo.repository;

import com.example.demo.entity.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> { // CrudRepository

  Account findByUsername(String username);

  Account findByEmail(String email);

  Optional<Account> findOneByUsername(String username);

  void deleteUserEntityByUsername(String username);

  List<Account> findByPhoneIsContaining(String phone);

  List<Account> findByEmailStartingWith(String email);
}
