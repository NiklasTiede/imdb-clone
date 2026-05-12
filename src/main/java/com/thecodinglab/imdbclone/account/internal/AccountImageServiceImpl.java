package com.thecodinglab.imdbclone.account.internal;

import com.thecodinglab.imdbclone.account.api.AccountImageService;
import com.thecodinglab.imdbclone.account.api.AccountImageToken;
import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.exception.domain.NotFoundException;
import com.thecodinglab.imdbclone.repository.AccountRepository;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AccountImageServiceImpl implements AccountImageService {

  private final AccountRepository accountRepository;

  public AccountImageServiceImpl(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Override
  public AccountImageToken getProfileImageToken(UserPrincipal currentUser) {
    return toImageToken(accountRepository.getAccount(currentUser));
  }

  @Override
  @Transactional
  public AccountImageToken updateProfileImageToken(Long accountId, String imageUrlToken) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(
                () -> new NotFoundException("User not found with id: %s".formatted(accountId)));
    account.setImageUrlToken(imageUrlToken);
    return toImageToken(accountRepository.save(account));
  }

  private AccountImageToken toImageToken(Account account) {
    return new AccountImageToken(account.getId(), account.getImageUrlToken());
  }
}
