package com.example.demo.service;

import com.example.demo.entity.Account;
import com.example.demo.payload.*;
import com.example.demo.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public interface AccountService {

  AccountSummaryResponse getCurrentAccount(UserPrincipal currentAccount);

  AccountProfile getAccountProfile(String username);

  Account createAccount(RegistrationRequest request, UserPrincipal currentAccount);

  Account updateAccount(String username, AccountRecord accountRecord, UserPrincipal currentAccount);

  MessageResponse deleteAccount(String username, UserPrincipal currentAccount);
}
