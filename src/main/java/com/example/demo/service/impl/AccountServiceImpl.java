package com.example.demo.service.impl;

import com.example.demo.Payload.AccountSummaryResponse;
import com.example.demo.Payload.CreateAccountRequest;
import com.example.demo.Payload.MessageResponse;
import com.example.demo.Payload.mapper.CustomCommentMapper;
import com.example.demo.entity.*;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.RatingRepository;
import com.example.demo.repository.WatchedMovieRepository;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AccountServiceImpl.class);

  private final AccountRepository accountRepository;
  private final WatchedMovieRepository watchedMovieRepository;
  private final RatingRepository ratingRepository;
  private final CommentRepository commentRepository;
  private final CustomCommentMapper commentMapper;

  public AccountServiceImpl(
      AccountRepository accountRepository,
      WatchedMovieRepository watchedMovieRepository,
      RatingRepository ratingRepository,
      CommentRepository commentRepository,
      CustomCommentMapper commentMapper) {
    this.accountRepository = accountRepository;
    this.watchedMovieRepository = watchedMovieRepository;
    this.ratingRepository = ratingRepository;
    this.commentRepository = commentRepository;
    this.commentMapper = commentMapper;
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
  public Account getProfile(String username) {

    LOGGER.info("the movie [{}] was saved successfully with movieId [{}].", "");

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
