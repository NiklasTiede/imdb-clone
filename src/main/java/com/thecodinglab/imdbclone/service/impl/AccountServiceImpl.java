package com.thecodinglab.imdbclone.service.impl;

import static com.thecodinglab.imdbclone.utility.Log.*;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static net.logstash.logback.argument.StructuredArguments.v;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.entity.Role;
import com.thecodinglab.imdbclone.exception.domain.UnauthorizedException;
import com.thecodinglab.imdbclone.payload.*;
import com.thecodinglab.imdbclone.payload.account.AccountProfile;
import com.thecodinglab.imdbclone.payload.account.AccountRecord;
import com.thecodinglab.imdbclone.payload.account.AccountSummaryResponse;
import com.thecodinglab.imdbclone.payload.account.UpdatedAccountProfile;
import com.thecodinglab.imdbclone.payload.authentication.RegistrationRequest;
import com.thecodinglab.imdbclone.payload.mapper.AccountMapper;
import com.thecodinglab.imdbclone.repository.AccountRepository;
import com.thecodinglab.imdbclone.repository.CommentRepository;
import com.thecodinglab.imdbclone.repository.RatingRepository;
import com.thecodinglab.imdbclone.repository.WatchedMovieRepository;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.service.AccountService;
import com.thecodinglab.imdbclone.service.RoleService;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

  private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);

  private final AccountRepository accountRepository;
  private final WatchedMovieRepository watchedMovieRepository;
  private final RatingRepository ratingRepository;
  private final CommentRepository commentRepository;
  private final PasswordEncoder passwordEncoder;
  private final RoleService roleService;
  private final AccountMapper accountMapper;

  public AccountServiceImpl(
      AccountRepository accountRepository,
      WatchedMovieRepository watchedMovieRepository,
      RatingRepository ratingRepository,
      CommentRepository commentRepository,
      PasswordEncoder passwordEncoder,
      RoleService roleService,
      AccountMapper accountMapper) {
    this.accountRepository = accountRepository;
    this.watchedMovieRepository = watchedMovieRepository;
    this.ratingRepository = ratingRepository;
    this.commentRepository = commentRepository;
    this.passwordEncoder = passwordEncoder;
    this.roleService = roleService;
    this.accountMapper = accountMapper;
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
  public AccountProfile getAccountProfile(String username) {
    Account account = accountRepository.getAccountByUsername(username);
    Long ratingsCount = ratingRepository.countRatingsByAccount(account);
    Long watchedMoviesCount = watchedMovieRepository.countWatchedMoviesByAccount(account);
    Long commentsCount = commentRepository.countCommentsByAccount(account);
    logger.info(
        "Account profile with id [{}] was retrieved from database.",
        v(ACCOUNT_ID, account.getId()));
    return new AccountProfile(
        account.getUsername(),
        account.getEmail(),
        account.getFirstName(),
        account.getLastName(),
        account.getPhone(),
        account.getBio(),
        account.getBirthday(),
        account.getImageUrlToken(),
        ratingsCount,
        watchedMoviesCount,
        commentsCount);
  }

  @Override
  public Account createAccount(RegistrationRequest request, UserPrincipal currentAccount) {
    String username = request.username().toLowerCase();
    String email = request.email().toLowerCase();
    String password = passwordEncoder.encode(request.password());
    Account account = new Account(username, email, password);
    account.setEnabled(true);
    List<Role> roles = roleService.giveRoleToRegisteredUser();
    account.setRoles(roles);
    Account savedAccount = accountRepository.save(account);
    logger.info(
        "Account with [{}] was created and activated.", kv(ACCOUNT_ID, savedAccount.getId()));
    return savedAccount;
  }

  @Override
  public UpdatedAccountProfile updateAccountProfile(
      String username, AccountRecord accountRecord, UserPrincipal currentAccount) {
    Account account = accountRepository.getAccountByUsername(username);
    if (Objects.equals(account.getId(), currentAccount.getId())
        || Boolean.TRUE.equals(UserPrincipal.isCurrentAccountAdmin(currentAccount))) {
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
          "Account with id ["
              + currentAccount.getId()
              + "] has no permission to update this resource.");
    }
  }

  @Override
  public MessageResponse deleteAccount(String username, UserPrincipal currentAccount) {
    Account account = accountRepository.getAccountByUsername(username);
    if (Objects.equals(account.getId(), currentAccount.getId())
        || Boolean.TRUE.equals(UserPrincipal.isCurrentAccountAdmin(currentAccount))) {
      accountRepository.delete(account);
      logger.info("Account with [{}] was deleted.", kv(ACCOUNT_ID, account.getId()));
      return new MessageResponse("Account with id [" + account.getId() + "] was deleted.");
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
