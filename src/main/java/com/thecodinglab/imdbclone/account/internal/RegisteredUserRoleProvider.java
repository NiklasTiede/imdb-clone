package com.thecodinglab.imdbclone.account.internal;

import com.thecodinglab.imdbclone.account.internal.persistence.AccountRepository;
import com.thecodinglab.imdbclone.account.internal.persistence.Role;
import com.thecodinglab.imdbclone.account.internal.persistence.RoleRepository;
import com.thecodinglab.imdbclone.enums.RoleNameEnum;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class RegisteredUserRoleProvider {

  private static final Logger logger = LoggerFactory.getLogger(RegisteredUserRoleProvider.class);

  private final AccountRepository accountRepository;
  private final RoleRepository roleRepository;

  RegisteredUserRoleProvider(AccountRepository accountRepository, RoleRepository roleRepository) {
    this.accountRepository = accountRepository;
    this.roleRepository = roleRepository;
  }

  List<Role> rolesForRegisteredUser() {
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
}
