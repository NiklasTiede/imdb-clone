package com.thecodinglab.imdbclone.account.internal;

import static com.thecodinglab.imdbclone.shared.logging.Log.*;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static net.logstash.logback.argument.StructuredArguments.v;

import com.thecodinglab.imdbclone.account.api.*;
import com.thecodinglab.imdbclone.account.api.events.AccountDeleted;
import com.thecodinglab.imdbclone.account.internal.mapper.AccountMapper;
import com.thecodinglab.imdbclone.account.internal.persistence.Account;
import com.thecodinglab.imdbclone.account.internal.persistence.AccountRepository;
import com.thecodinglab.imdbclone.account.internal.persistence.LocalCredential;
import com.thecodinglab.imdbclone.account.internal.persistence.LocalCredentialRepository;
import com.thecodinglab.imdbclone.engagement.api.AccountActivityService;
import com.thecodinglab.imdbclone.engagement.api.EngagementStats;
import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.error.UnauthorizedException;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountManagement implements AccountService {

  private static final Logger logger = LoggerFactory.getLogger(AccountManagement.class);

  private final AccountRepository accountRepository;
  private final LocalCredentialRepository localCredentialRepository;
  private final AccountActivityService accountActivityService;
  private final PasswordEncoder passwordEncoder;
  private final RegisteredUserRoleProvider registeredUserRoleProvider;
  private final AccountMapper accountMapper;
  private final ApplicationEventPublisher events;

  public AccountManagement(
      AccountRepository accountRepository,
      LocalCredentialRepository localCredentialRepository,
      AccountActivityService accountActivityService,
      PasswordEncoder passwordEncoder,
      RegisteredUserRoleProvider registeredUserRoleProvider,
      AccountMapper accountMapper,
      ApplicationEventPublisher events) {
    this.accountRepository = accountRepository;
    this.localCredentialRepository = localCredentialRepository;
    this.accountActivityService = accountActivityService;
    this.passwordEncoder = passwordEncoder;
    this.registeredUserRoleProvider = registeredUserRoleProvider;
    this.accountMapper = accountMapper;
    this.events = events;
  }

  @Override
  public AccountSummaryResponse getCurrentAccount(UserPrincipal currentAccount) {
    return new AccountSummaryResponse(
        currentAccount.getId(),
        currentAccount.getUsername(),
        currentAccount.getEmail(),
        currentAccount.getFirstName(),
        currentAccount.getLastName());
  }

  @Override
  public AccountProfile getCurrentAccountProfile(UserPrincipal currentAccount) {
    Account account = accountRepository.getAccount(currentAccount);
    ProfileCounts counts = getProfileCounts(account);
    return new AccountProfile(
        account.getUsername(),
        account.getEmail(),
        account.getFirstName(),
        account.getLastName(),
        account.getPhone(),
        account.getBio(),
        account.getBirthday(),
        account.getImageUrlToken(),
        counts.ratingsCount(),
        counts.watchedMoviesCount(),
        counts.commentsCount());
  }

  @Override
  public PublicAccountProfile getAccountProfile(String username) {
    Account account = accountRepository.getAccountByUsername(username);
    ProfileCounts counts = getProfileCounts(account);
    return new PublicAccountProfile(
        account.getUsername(),
        account.getFirstName(),
        account.getLastName(),
        account.getBio(),
        account.getImageUrlToken(),
        counts.ratingsCount(),
        counts.watchedMoviesCount(),
        counts.commentsCount());
  }

  @Override
  public List<PublicAccountSummary> getPublicAccountSummaries(List<Long> accountIds) {
    var uniqueAccountIds = new LinkedHashSet<>(accountIds);
    Map<Long, Account> accountsById =
        accountRepository.findAllById(uniqueAccountIds).stream()
            .collect(Collectors.toMap(Account::getId, Function.identity()));

    return uniqueAccountIds.stream()
        .map(accountsById::get)
        .filter(Objects::nonNull)
        .map(
            account ->
                new PublicAccountSummary(
                    account.getId(),
                    account.getUsername(),
                    getDisplayName(account),
                    account.getImageUrlToken()))
        .toList();
  }

  private String getDisplayName(Account account) {
    return java.util.stream.Stream.of(account.getFirstName(), account.getLastName())
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(namePart -> !namePart.isEmpty())
        .collect(Collectors.joining(" "));
  }

  private ProfileCounts getProfileCounts(Account account) {
    EngagementStats stats = accountActivityService.getStatsForAccount(account.getId());
    logger.info(
        "Account profile with id [{}] was retrieved from database.",
        v(ACCOUNT_ID, account.getId()));
    return new ProfileCounts(
        stats.ratingsCount(), stats.watchedMoviesCount(), stats.commentsCount());
  }

  private record ProfileCounts(Long ratingsCount, Long watchedMoviesCount, Long commentsCount) {}

  @Override
  public AccountCreated createAccount(CreateAccountRequest request, UserPrincipal currentAccount) {
    String username = request.username().toLowerCase();
    String email = request.email().toLowerCase();
    String password = passwordEncoder.encode(request.password());
    Account account = new Account(username, email);
    account.setEnabled(true);
    account.setRoles(registeredUserRoleProvider.rolesForRegisteredUser());
    Account savedAccount = accountRepository.save(account);
    localCredentialRepository.save(new LocalCredential(savedAccount.getId(), password));
    logger.info(
        "Account with [{}] was created and activated.", kv(ACCOUNT_ID, savedAccount.getId()));
    return new AccountCreated(savedAccount.getUsername(), savedAccount.getEmail());
  }

  @Override
  public UpdatedAccountProfile updateAccountProfile(
      String username, AccountRecord accountRecord, UserPrincipal currentAccount) {
    Account account = accountRepository.getAccountByUsername(username);
    if (Objects.equals(account.getId(), currentAccount.getId())
        || UserPrincipal.isCurrentAccountAdmin(currentAccount)) {
      String firstName =
          accountRecord.firstName() != null ? accountRecord.firstName().toLowerCase() : null;
      account.setFirstName(firstName);
      String lastName =
          accountRecord.lastName() != null ? accountRecord.lastName().toLowerCase() : null;
      account.setLastName(lastName);
      account.setPhone(accountRecord.phone());
      account.setBirthday(accountRecord.birthday());
      account.setBio(accountRecord.bio());
      Account updatedAccount = accountRepository.save(account);
      logger.info("Account with [{}] was updated.", kv(ACCOUNT_ID, updatedAccount.getId()));
      return accountMapper.entityToUpdatedProfile(updatedAccount);
    } else {
      logger.warn(
          "User with [{}] tried to update an account without ADMIN permissions.",
          kv(ACCOUNT_ID, currentAccount.getId()));
      throw new UnauthorizedException(
          "Account with id [%d] has no permission to update this resource."
              .formatted(currentAccount.getId()));
    }
  }

  @Override
  @Transactional
  public MessageResponse deleteAccount(String username, UserPrincipal currentAccount) {
    Account account = accountRepository.getAccountByUsername(username);
    if (Objects.equals(account.getId(), currentAccount.getId())
        || UserPrincipal.isCurrentAccountAdmin(currentAccount)) {
      String imageUrlToken = account.getImageUrlToken();
      accountRepository.delete(account);
      events.publishEvent(new AccountDeleted(account.getId(), imageUrlToken));
      logger.info("Account with [{}] was deleted.", kv(ACCOUNT_ID, account.getId()));
      return new MessageResponse("Account with id [%d] was deleted.".formatted(account.getId()));
    } else {
      logger.warn(
          "User with [{}] tried to delete an account without ADMIN permissions.",
          kv(ACCOUNT_ID, currentAccount.getId()));
      throw new UnauthorizedException(
          "Account with id [%s] has no permission to delete this resource."
              .formatted(currentAccount.getId()));
    }
  }
}
