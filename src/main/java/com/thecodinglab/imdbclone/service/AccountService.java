package com.thecodinglab.imdbclone.service;

import com.thecodinglab.imdbclone.identity.api.RegistrationRequest;
import com.thecodinglab.imdbclone.identity.api.UserPrincipal;
import com.thecodinglab.imdbclone.payload.*;
import com.thecodinglab.imdbclone.payload.account.*;

public interface AccountService {

  AccountSummaryResponse getCurrentAccount(UserPrincipal currentAccount);

  AccountProfile getCurrentAccountProfile(UserPrincipal currentAccount);

  PublicAccountProfile getAccountProfile(String username);

  AccountCreated createAccount(RegistrationRequest request, UserPrincipal currentAccount);

  UpdatedAccountProfile updateAccountProfile(
      String username, AccountRecord accountRecord, UserPrincipal currentAccount);

  MessageResponse deleteAccount(String username, UserPrincipal currentAccount);
}
