package com.example.demo.service.impl;

import com.example.demo.Payload.AccountSummaryResponse;
import com.example.demo.Payload.CreateAccountRequest;
import com.example.demo.Payload.MessageResponse;
import com.example.demo.entity.Account;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Rating;
import com.example.demo.entity.WatchedMovie;
import com.example.demo.repository.AccountRepository;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.AccountService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AccountServiceImpl.class);

  private final AccountRepository accountRepository;

  public AccountServiceImpl(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
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
  public Account getProfile(String username, UserPrincipal currentAccount) {

    return null;
  }

  @Override
  public List<Comment> getCommentsByAccount(String username, UserPrincipal currentAccoun) {
    return null;
  }

  @Override
  public List<WatchedMovie> getWatchedMoviesByAccount(
      String username, UserPrincipal currentAccount) {
    return null;
  }

  @Override
  public List<Rating> getRatingsByAccount(String username, UserPrincipal currentAccount) {
    return null;
  }

  @Override
  public Account updateAccount(String username, UserPrincipal currentAccount) {
    return null;
  }

  @Override
  public MessageResponse deleteAccount(String username, UserPrincipal currentAccount) {
    return null;
  }

  @Override
  public Account createAccount(CreateAccountRequest request, UserPrincipal currentAccount) {
    return null;
  }

  @Override
  public MessageResponse giveAdminRole(String username, UserPrincipal currentAccount) {
    return null;
  }

  @Override
  public MessageResponse takeAdminRole(String username, UserPrincipal currentAccount) {
    return null;
  }
}
