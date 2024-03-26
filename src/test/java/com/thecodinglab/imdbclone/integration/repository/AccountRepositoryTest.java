package com.thecodinglab.imdbclone.integration.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.integration.BaseContainers;
import com.thecodinglab.imdbclone.repository.AccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
class AccountRepositoryTest extends BaseContainers {

  @Autowired private AccountRepository accountRepository;

  @Test
  void saveAccount() {
    // arrange
    var account = new Account();
    account.setLastName("Doe");
    account.setUsername("the_best");
    account.setEmail("john.doe@example.com");
    account.setPassword("best4all");

    // act
    accountRepository.save(account);
    Optional<Account> foundAccount = accountRepository.findById(account.getId());

    // assert
    assertTrue(foundAccount.isPresent());
    assertEquals("Doe", foundAccount.get().getLastName());
    assertEquals("the_best", foundAccount.get().getUsername());
    assertNotEquals("bad", foundAccount.get().getPassword());
  }

  @Test
  void getAccountByUsername() {}
}
