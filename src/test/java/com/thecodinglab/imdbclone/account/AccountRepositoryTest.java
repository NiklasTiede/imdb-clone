package com.thecodinglab.imdbclone.account;

import static org.junit.jupiter.api.Assertions.*;

import com.thecodinglab.imdbclone.account.internal.persistence.Account;
import com.thecodinglab.imdbclone.account.internal.persistence.AccountRepository;
import com.thecodinglab.imdbclone.shared.error.NotFoundException;
import com.thecodinglab.imdbclone.support.BaseContainers;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
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
  void getAccountByUsername_success() {
    var account = accountRepository.getAccountByUsername("test_user_one");

    assertEquals(1L, account.getId());
    assertEquals("test_user_one", account.getUsername());
    assertEquals("one@gmail.com", account.getEmail());
  }

  @Test
  void getAccountByUsername_notFound() {
    var exception =
        assertThrows(
            NotFoundException.class, () -> accountRepository.getAccountByUsername("missing_user"));

    assertEquals(
        "User with username [missing_user] not found in database.", exception.getMessage());
  }
}
