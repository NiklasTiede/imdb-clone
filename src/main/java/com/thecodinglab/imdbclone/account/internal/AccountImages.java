package com.thecodinglab.imdbclone.account.internal;

import com.thecodinglab.imdbclone.account.api.AccountImageService;
import com.thecodinglab.imdbclone.account.api.AccountImageToken;
import com.thecodinglab.imdbclone.account.api.events.ProfileImageReplaced;
import com.thecodinglab.imdbclone.account.internal.persistence.Account;
import com.thecodinglab.imdbclone.account.internal.persistence.AccountRepository;
import com.thecodinglab.imdbclone.shared.error.NotFoundException;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class AccountImages implements AccountImageService {

  private final AccountRepository accountRepository;
  private final EntityManager entityManager;
  private final ApplicationEventPublisher events;

  public AccountImages(
      AccountRepository accountRepository,
      EntityManager entityManager,
      ApplicationEventPublisher events) {
    this.accountRepository = accountRepository;
    this.entityManager = entityManager;
    this.events = events;
  }

  @Override
  public AccountImageToken getProfileImageToken(UserPrincipal currentUser) {
    return toImageToken(accountRepository.getAccount(currentUser));
  }

  @Override
  @Transactional
  public AccountImageToken updateProfileImageToken(Long accountId, String imageUrlToken) {
    Account account = getAccount(accountId);
    entityManager.flush();
    try {
      entityManager.refresh(account, LockModeType.PESSIMISTIC_WRITE);
    } catch (EntityNotFoundException exception) {
      throw new NotFoundException("User not found with id: %s".formatted(accountId));
    }
    String previous = account.getImageUrlToken();
    account.setImageUrlToken(imageUrlToken);
    AccountImageToken result = toImageToken(accountRepository.save(account));
    if (!Objects.equals(previous, imageUrlToken)) {
      events.publishEvent(new ProfileImageReplaced(accountId, previous, imageUrlToken));
    }
    return result;
  }

  @Override
  @Transactional
  public void clearProfileImageToken(Long accountId) {
    updateProfileImageToken(accountId, null);
  }

  @Override
  public boolean isProfileImageTokenReferenced(String token) {
    return accountRepository.existsByImageUrlToken(token);
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
