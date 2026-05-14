package com.thecodinglab.imdbclone.account.internal;

import com.thecodinglab.imdbclone.account.api.AccountImageService;
import com.thecodinglab.imdbclone.account.api.AccountImageToken;
import com.thecodinglab.imdbclone.account.internal.persistence.Account;
import com.thecodinglab.imdbclone.account.internal.persistence.AccountRepository;
import com.thecodinglab.imdbclone.shared.error.NotFoundException;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AccountImages implements AccountImageService {

  private final AccountRepository accountRepository;

  public AccountImages(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Override
  public AccountImageToken getProfileImageToken(UserPrincipal currentUser) {
    return toImageToken(accountRepository.getAccount(currentUser));
  }

  @Override
  @Transactional
  public AccountImageToken updateProfileImageToken(Long accountId, String imageUrlToken) {
    Account account = getAccount(accountId);
    account.setImageUrlToken(imageUrlToken);
    return toImageToken(accountRepository.save(account));
  }

  @Override
  @Transactional
  public void clearProfileImageToken(Long accountId) {
    Account account = getAccount(accountId);
    account.setImageUrlToken(null);
    accountRepository.save(account);
  }

  private Account getAccount(Long accountId) {
    return accountRepository
        .findById(accountId)
        .orElseThrow(
            () -> new NotFoundException("User not found with id: %s".formatted(accountId)));
  }

  private AccountImageToken toImageToken(Account account) {
    return new AccountImageToken(account.getId(), account.getImageUrlToken());
  }
}
