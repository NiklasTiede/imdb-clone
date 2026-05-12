package com.thecodinglab.imdbclone.account.api;

public interface AccountIdentityService {

  boolean isUsernameAvailable(String username);

  boolean isEmailAvailable(String email);

  AccountIdentity createAccountForIdentity(
      String username, String email, String passwordHash, boolean enabled);

  AccountIdentity findByEmail(String email);

  AccountIdentity enableAccount(Long accountId);

  void updatePassword(Long accountId, String passwordHash);

  AccountCredentials loadCredentialsByUsernameOrEmail(String usernameOrEmail);

  AccountCredentials loadCredentialsById(Long accountId);
}
