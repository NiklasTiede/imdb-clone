package com.thecodinglab.imdbclone.validation;

import com.thecodinglab.imdbclone.repository.AccountRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AvailableUsernameImpl implements ConstraintValidator<AvailableUsername, String> {

  private final AccountRepository accountRepository;

  public AvailableUsernameImpl(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Override
  public void initialize(AvailableUsername constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(String username, ConstraintValidatorContext context) {
    return !accountRepository.existsByUsername(username);
  }
}
