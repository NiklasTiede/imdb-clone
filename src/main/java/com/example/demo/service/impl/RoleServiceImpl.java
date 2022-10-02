package com.example.demo.service.impl;

import com.example.demo.entity.Role;
import com.example.demo.enums.RoleNameEnum;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.service.RoleService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl implements RoleService {

  private static final String USER_ROLE_NOT_SET = "User role not set";
  private static final String ADMIN_ROLE_NOT_SET = "Admin role not set";

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
      roles.add(
          roleRepository
              .findByName(RoleNameEnum.ROLE_USER)
              .orElseThrow(() -> new NotFoundException(USER_ROLE_NOT_SET)));
      roles.add(
          roleRepository
              .findByName(RoleNameEnum.ROLE_ADMIN)
              .orElseThrow(() -> new NotFoundException(ADMIN_ROLE_NOT_SET)));
      LOGGER.info("First user was created and admin role was added.");
    } else {
      roles.add(
          roleRepository
              .findByName(RoleNameEnum.ROLE_USER)
              .orElseThrow(() -> new NotFoundException(USER_ROLE_NOT_SET)));
    }
    return roles;
  }
}
