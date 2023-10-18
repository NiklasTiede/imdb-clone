package com.thecodinglab.imdbclone.service.impl;

import static com.thecodinglab.imdbclone.utility.Log.ACCOUNT_ID;
import static net.logstash.logback.argument.StructuredArguments.kv;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.entity.Role;
import com.thecodinglab.imdbclone.enums.RoleNameEnum;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.repository.AccountRepository;
import com.thecodinglab.imdbclone.repository.RoleRepository;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.service.RoleService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl implements RoleService {

  private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

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
      logger.info("First user was created and admin role was added.");
    } else {
      roles.add(roleRepository.getRoleByRoleEnum(RoleNameEnum.ROLE_USER));
    }
    return roles;
  }

  @Override
  public MessageResponse giveAdminRole(String username, UserPrincipal currentAccount) {
    Account account = accountRepository.getAccountByUsername(username);
    Collection<Role> roles = account.getRoles();
    Role adminRole = roleRepository.getRoleByRoleEnum(RoleNameEnum.ROLE_ADMIN);
    roles.add(adminRole);
    account.setRoles(roles);
    Account updatedAccount = accountRepository.save(account);
    logger.info(
        "Account with [{}] was given ADMIN permission.", kv(ACCOUNT_ID, updatedAccount.getId()));
    return new MessageResponse(
        "Account with id [" + updatedAccount.getId() + "] was given ADMIN permission.");
  }

  @Override
  public MessageResponse removeAdminRole(String username, UserPrincipal currentAccount) {
    Account account = accountRepository.getAccountByUsername(username);
    Collection<Role> roles = account.getRoles();
    Role adminRole = roleRepository.getRoleByRoleEnum(RoleNameEnum.ROLE_ADMIN);
    roles.remove(adminRole);
    account.setRoles(roles);
    Account updatedAccount = accountRepository.save(account);
    logger.info(
        "Account with id [{}] was taken ADMIN permission.", kv(ACCOUNT_ID, updatedAccount.getId()));
    return new MessageResponse(
        "Account with id [" + updatedAccount.getId() + "] was taken ADMIN permission.");
  }
}
