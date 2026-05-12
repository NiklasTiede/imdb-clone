package com.thecodinglab.imdbclone.account.internal;

import com.thecodinglab.imdbclone.account.api.AccountCredentials;
import com.thecodinglab.imdbclone.account.api.AccountIdentity;
import com.thecodinglab.imdbclone.account.api.AccountIdentityService;
import com.thecodinglab.imdbclone.account.internal.persistence.Account;
import com.thecodinglab.imdbclone.account.internal.persistence.AccountRepository;
import com.thecodinglab.imdbclone.exception.domain.NotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AccountIdentityServiceImpl implements AccountIdentityService {

  private final AccountRepository accountRepository;
  private final RegisteredUserRoleProvider registeredUserRoleProvider;

  public AccountIdentityServiceImpl(
      AccountRepository accountRepository, RegisteredUserRoleProvider registeredUserRoleProvider) {
    this.accountRepository = accountRepository;
    this.registeredUserRoleProvider = registeredUserRoleProvider;
  }

  @Override
  public boolean isUsernameAvailable(String username) {
    return !accountRepository.existsByUsername(username);
  }

  @Override
  public boolean isEmailAvailable(String email) {
    return !accountRepository.existsByEmail(email);
  }

  @Override
  @Transactional
  public AccountIdentity createAccountForIdentity(
      String username, String email, String passwordHash, boolean enabled) {
    Account account = new Account(username.toLowerCase(), email.toLowerCase(), passwordHash);
    account.setEnabled(enabled);
    account.setRoles(registeredUserRoleProvider.rolesForRegisteredUser());
    return toIdentity(accountRepository.save(account));
  }

  @Override
  public AccountIdentity findByEmail(String email) {
    return accountRepository
        .findByEmail(email)
        .map(this::toIdentity)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Account with email address [%s] not found in database.".formatted(email)));
  }

  @Override
  public AccountIdentity findByUsername(String username) {
    return toIdentity(accountRepository.getAccountByUsername(username));
  }

  @Override
  @Transactional
  public AccountIdentity enableAccount(Long accountId) {
    Account account = getAccount(accountId);
    account.setEnabled(true);
    return toIdentity(accountRepository.save(account));
  }

  @Override
  @Transactional
  public void updatePassword(Long accountId, String passwordHash) {
    Account account = getAccount(accountId);
    account.setPassword(passwordHash);
    accountRepository.save(account);
  }

  @Override
  public AccountCredentials loadCredentialsByUsernameOrEmail(String usernameOrEmail) {
    Account account =
        accountRepository
            .findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "User not found with this username or email: %s"
                            .formatted(usernameOrEmail)));
    return toCredentials(account);
  }

  @Override
  public AccountCredentials loadCredentialsById(Long accountId) {
    return toCredentials(getAccount(accountId));
  }

  private Account getAccount(Long accountId) {
    return accountRepository
        .findById(accountId)
        .orElseThrow(
            () -> new NotFoundException("User not found with id: %s".formatted(accountId)));
  }

  private AccountIdentity toIdentity(Account account) {
    return new AccountIdentity(account.getId(), account.getUsername(), account.getEmail());
  }

  private AccountCredentials toCredentials(Account account) {
    return new AccountCredentials(
        account.getId(),
        account.getFirstName(),
        account.getLastName(),
        account.getUsername(),
        account.getEmail(),
        account.getPassword(),
        account.getLocked(),
        account.getEnabled(),
        account.getRoles().stream().map(role -> role.getName().name()).toList());
  }
}
