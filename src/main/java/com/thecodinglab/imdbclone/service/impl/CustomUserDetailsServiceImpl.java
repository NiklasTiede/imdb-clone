package com.thecodinglab.imdbclone.service.impl;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.repository.AccountRepository;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.service.CustomUserDetailsService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsServiceImpl implements UserDetailsService, CustomUserDetailsService {

  private final AccountRepository accountRepository;

  @Autowired
  public CustomUserDetailsServiceImpl(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Override
  @Transactional
  public UserDetails loadUserByUsername(String usernameOrEmail) {
    Account account =
        accountRepository
            .findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            .orElseThrow(
                () ->
                    new UsernameNotFoundException(
                        String.format(
                            "User not found with this username or email: %s", usernameOrEmail)));
    return UserPrincipal.create(account);
  }

  @Override
  @Transactional
  public UserDetails loadUserById(Long id) {
    Account account =
        accountRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new UsernameNotFoundException(String.format("User not found with id: %s", id)));
    return UserPrincipal.create(account);
  }
}
