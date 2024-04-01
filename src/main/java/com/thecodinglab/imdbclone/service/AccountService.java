package com.thecodinglab.imdbclone.service;

import com.thecodinglab.imdbclone.payload.*;
import com.thecodinglab.imdbclone.payload.account.*;
import com.thecodinglab.imdbclone.payload.authentication.RegistrationRequest;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public interface AccountService {

  AccountSummaryResponse getCurrentAccount(UserPrincipal currentAccount);

  AccountProfile getAccountProfile(String username);

  AccountCreated createAccount(RegistrationRequest request, UserPrincipal currentAccount);

  UpdatedAccountProfile updateAccountProfile(
      String username, AccountRecord accountRecord, UserPrincipal currentAccount);

  MessageResponse deleteAccount(String username, UserPrincipal currentAccount);
}
