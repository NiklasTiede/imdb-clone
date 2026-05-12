package com.thecodinglab.imdbclone.account.api;

import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.security.UserPrincipal;

public interface AccountService {

  AccountSummaryResponse getCurrentAccount(UserPrincipal currentAccount);

  AccountProfile getCurrentAccountProfile(UserPrincipal currentAccount);

  PublicAccountProfile getAccountProfile(String username);

  AccountCreated createAccount(RegistrationRequest request, UserPrincipal currentAccount);

  UpdatedAccountProfile updateAccountProfile(
      String username, AccountRecord accountRecord, UserPrincipal currentAccount);

  MessageResponse deleteAccount(String username, UserPrincipal currentAccount);
}
