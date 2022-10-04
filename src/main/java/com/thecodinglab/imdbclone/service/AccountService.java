package com.thecodinglab.imdbclone.service;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.payload.*;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public interface AccountService {

  AccountSummaryResponse getCurrentAccount(UserPrincipal currentAccount);

  AccountProfile getAccountProfile(String username);

  Account createAccount(RegistrationRequest request, UserPrincipal currentAccount);

  Account updateAccount(String username, AccountRecord accountRecord, UserPrincipal currentAccount);

  MessageResponse deleteAccount(String username, UserPrincipal currentAccount);
}
