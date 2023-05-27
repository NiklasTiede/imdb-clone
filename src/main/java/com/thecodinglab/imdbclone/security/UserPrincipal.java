package com.thecodinglab.imdbclone.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.enums.RoleNameEnum;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {

  private final Long id;
  private final String firstName;
  private final String lastName;
  private final String username;
  @JsonIgnore private final String email;
  @JsonIgnore private final String password;
  private final boolean locked;
  private final boolean enabled;
  private final Collection<? extends GrantedAuthority> authorities;

  public UserPrincipal(
      Long id,
      String firstName,
      String lastName,
      String username,
      String email,
      String password,
      boolean locked,
      boolean enabled,
      Collection<? extends GrantedAuthority> authorities) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.username = username;
    this.email = email;
    this.password = password;
    this.locked = locked;
    this.enabled = enabled;

    if (authorities == null) {
      this.authorities = null;
    } else {
      this.authorities = new ArrayList<>(authorities);
    }
  }

  public static UserPrincipal create(Account account) {
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

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities == null ? null : new ArrayList<>(authorities);
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return !locked;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public boolean equals(Object object) {
    if (this == object) return true;
    if (object == null || getClass() != object.getClass()) return false;
    UserPrincipal that = (UserPrincipal) object;
    return Objects.equals(id, that.id);
  }

  public int hashCode() {
    return Objects.hash(id);
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public static boolean isCurrentAccountAdmin(UserPrincipal currentAccount) {
    return currentAccount
        .getAuthorities()
        .contains(new SimpleGrantedAuthority(RoleNameEnum.ROLE_ADMIN.toString()));
  }
}
