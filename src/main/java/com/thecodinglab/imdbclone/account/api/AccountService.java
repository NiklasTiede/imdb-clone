package com.thecodinglab.imdbclone.account.api;

import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import java.util.List;

public interface AccountService {

  AccountSummaryResponse getCurrentAccount(UserPrincipal currentAccount);

  AccountProfile getCurrentAccountProfile(UserPrincipal currentAccount);

  PublicAccountProfile getAccountProfile(String username);

  List<PublicAccountSummary> getPublicAccountSummaries(List<Long> accountIds);

  AccountCreated createAccount(CreateAccountRequest request, UserPrincipal currentAccount);

  UpdatedAccountProfile updateAccountProfile(
      String username, AccountRecord accountRecord, UserPrincipal currentAccount);

  MessageResponse deleteAccount(String username, UserPrincipal currentAccount);
}
