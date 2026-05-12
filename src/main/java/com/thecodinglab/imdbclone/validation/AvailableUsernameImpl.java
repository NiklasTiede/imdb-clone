package com.thecodinglab.imdbclone.validation;

import com.thecodinglab.imdbclone.account.api.AccountIdentityService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AvailableUsernameImpl implements ConstraintValidator<AvailableUsername, String> {

  private final AccountIdentityService accountIdentityService;

  public AvailableUsernameImpl(AccountIdentityService accountIdentityService) {
    this.accountIdentityService = accountIdentityService;
  }

  @Override
  public void initialize(AvailableUsername constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(String username, ConstraintValidatorContext context) {
    return accountIdentityService.isUsernameAvailable(username);
  }
}
