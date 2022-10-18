package com.thecodinglab.imdbclone.validation;

import com.thecodinglab.imdbclone.exception.BadRequestException;
import com.thecodinglab.imdbclone.repository.AccountRepository;
import org.springframework.stereotype.Component;

@Component
public class UniqueValidation {

  private static AccountRepository accountRepository;

  public UniqueValidation(AccountRepository accountRepository) {
    UniqueValidation.accountRepository = accountRepository;
  }

  public static void isUsernameAndEmailValid(String username, String email) {
    if (Boolean.TRUE.equals(accountRepository.existsByUsername(username))) {
      throw new BadRequestException("Username is already taken");
    }
    if (Boolean.TRUE.equals(accountRepository.existsByEmail(email))) {
      throw new BadRequestException("Email is already taken");
    }
  }
}
