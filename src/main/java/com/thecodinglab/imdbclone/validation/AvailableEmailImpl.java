package com.thecodinglab.imdbclone.validation;

import com.thecodinglab.imdbclone.account.api.AccountIdentityService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AvailableEmailImpl implements ConstraintValidator<AvailableEmail, String> {

  private final AccountIdentityService accountIdentityService;

  public AvailableEmailImpl(AccountIdentityService accountIdentityService) {
    this.accountIdentityService = accountIdentityService;
  }

  @Override
  public void initialize(AvailableEmail constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(String email, ConstraintValidatorContext context) {
    return accountIdentityService.isEmailAvailable(email);
  }
}
