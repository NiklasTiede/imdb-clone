package com.thecodinglab.imdbclone.identity.internal.security;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.identity.api.UserPrincipal;
import com.thecodinglab.imdbclone.repository.AccountRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsServiceImpl implements UserDetailsService, CustomUserDetailsService {

  private final AccountRepository accountRepository;

  public CustomUserDetailsServiceImpl(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Override
  @Transactional
  public @NonNull UserDetails loadUserByUsername(String usernameOrEmail) {
    Account account =
        accountRepository
            .findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            .orElseThrow(
                () ->
                    new UsernameNotFoundException(
                        "User not found with this username or email: %s"
                            .formatted(usernameOrEmail)));
    return createUserPrincipal(account);
  }

  @Override
  @Transactional
  public UserDetails loadUserById(Long id) {
    Account account =
        accountRepository
            .findById(id)
            .orElseThrow(
                () -> new UsernameNotFoundException("User not found with id: %s".formatted(id)));
    return createUserPrincipal(account);
  }

  private UserPrincipal createUserPrincipal(Account account) {
    List<SimpleGrantedAuthority> authorities =
        account.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getName().name()))
            .toList();

    return new UserPrincipal(
        account.getId(),
        account.getFirstName(),
        account.getLastName(),
        account.getUsername(),
        account.getEmail(),
        account.getPassword(),
        account.getLocked(),
        account.getEnabled(),
        authorities);
  }
}
