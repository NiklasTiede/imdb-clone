package com.example.demo.service.impl;

import com.example.demo.entity.Account;
import com.example.demo.entity.Role;
import com.example.demo.enums.RoleNameEnum;
import com.example.demo.exceptions.UnauthorizedException;
import com.example.demo.payload.MessageResponse;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.RoleService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl implements RoleService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RoleServiceImpl.class);

  private final AccountRepository accountRepository;
  private final RoleRepository roleRepository;

  public RoleServiceImpl(AccountRepository accountRepository, RoleRepository roleRepository) {
    this.accountRepository = accountRepository;
    this.roleRepository = roleRepository;
  }

  @Override
  public List<Role> giveRoleToRegisteredUser() {
    List<Role> roles = new ArrayList<>();
    if (accountRepository.count() == 0) {
      roles.add(roleRepository.getRoleByRoleEnum(RoleNameEnum.ROLE_USER));
      roles.add(roleRepository.getRoleByRoleEnum(RoleNameEnum.ROLE_ADMIN));
      LOGGER.info("First user was created and admin role was added.");
    } else {
      roles.add(roleRepository.getRoleByRoleEnum(RoleNameEnum.ROLE_USER));
    }
    return roles;
  }

  @Override
  public MessageResponse giveAdminRole(String username, UserPrincipal currentAccount) {
    Account account = accountRepository.getAccountByUsername(username);
    if (Objects.equals(account.getId(), currentAccount.getId())
        || UserPrincipal.isCurrentAccountAdmin(currentAccount)) {
      Collection<Role> roles = account.getRoles();
      Role adminRole = roleRepository.getRoleByRoleEnum(RoleNameEnum.ROLE_ADMIN);
      roles.add(adminRole);
      account.setRoles(roles);
      Account updatedAccount = accountRepository.save(account);
      LOGGER.info("Account with id [{}] was given ADMIN permission.", updatedAccount.getId());
      return new MessageResponse(
          "Account with id [" + updatedAccount.getId() + "] was given ADMIN permission.");
    } else {
      LOGGER.warn(
          "User with accountId [{}] and no ADMIN permission tried to give another user ADMIN permission.",
          currentAccount.getId());
      throw new UnauthorizedException(
          "Account with id ["
              + currentAccount.getId()
              + "] has no permission to give ADMIN permission.");
    }
  }

  @Override
  public MessageResponse removeAdminRole(String username, UserPrincipal currentAccount) {
    Account account = accountRepository.getAccountByUsername(username);
    if (Objects.equals(account.getId(), currentAccount.getId())
        || UserPrincipal.isCurrentAccountAdmin(currentAccount)) {
      Collection<Role> roles = account.getRoles();
      Role adminRole = roleRepository.getRoleByRoleEnum(RoleNameEnum.ROLE_ADMIN);
      roles.remove(adminRole);
      account.setRoles(roles);
      Account updatedAccount = accountRepository.save(account);
      LOGGER.info("Account with id [{}] was taken ADMIN permission.", updatedAccount.getId());
      return new MessageResponse(
          "Account with id [" + updatedAccount.getId() + "] was taken ADMIN permission.");
    } else {
      LOGGER.warn(
          "Account with id [{}] and no ADMIN permission tried to take another accounts's ADMIN permission.",
          currentAccount.getId());
      throw new UnauthorizedException(
          "Account with id ["
              + currentAccount.getId()
              + "] has no permission to take ADMIN permission.");
    }
  }
}
