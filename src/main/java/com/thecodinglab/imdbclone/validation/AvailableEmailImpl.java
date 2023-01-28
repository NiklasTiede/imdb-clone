package com.thecodinglab.imdbclone.validation;

import com.thecodinglab.imdbclone.repository.AccountRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AvailableEmailImpl implements ConstraintValidator<AvailableEmail, String> {

  private final AccountRepository accountRepository;

  public AvailableEmailImpl(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Override
  public void initialize(AvailableEmail constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(String email, ConstraintValidatorContext context) {
    return !accountRepository.existsByEmail(email);
  }
}
