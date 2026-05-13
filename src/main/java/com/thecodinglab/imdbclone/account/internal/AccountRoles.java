package com.thecodinglab.imdbclone.account.internal;

import static com.thecodinglab.imdbclone.shared.logging.Log.ACCOUNT_ID;
import static net.logstash.logback.argument.StructuredArguments.kv;

import com.thecodinglab.imdbclone.account.api.RoleService;
import com.thecodinglab.imdbclone.account.internal.persistence.Account;
import com.thecodinglab.imdbclone.account.internal.persistence.AccountRepository;
import com.thecodinglab.imdbclone.account.internal.persistence.Role;
import com.thecodinglab.imdbclone.account.internal.persistence.RoleName;
import com.thecodinglab.imdbclone.account.internal.persistence.RoleRepository;
import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import jakarta.transaction.Transactional;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountRoles implements RoleService {

  private static final Logger logger = LoggerFactory.getLogger(AccountRoles.class);

  private final AccountRepository accountRepository;
  private final RoleRepository roleRepository;

  public AccountRoles(AccountRepository accountRepository, RoleRepository roleRepository) {
    this.accountRepository = accountRepository;
    this.roleRepository = roleRepository;
  }

  @Override
  @Transactional
  public MessageResponse giveAdminRole(String username, UserPrincipal currentAccount) {
    Account account = accountRepository.getAccountByUsername(username);
    Collection<Role> roles = account.getRoles();
    Role adminRole = roleRepository.getRoleByRoleName(RoleName.ROLE_ADMIN);
    roles.add(adminRole);
    account.setRoles(roles);
    Account updatedAccount = accountRepository.save(account);
    logger.info(
        "Account with [{}] was given ADMIN permission.", kv(ACCOUNT_ID, updatedAccount.getId()));
    return new MessageResponse(
        "Account with id [%d] was given ADMIN permission.".formatted(updatedAccount.getId()));
  }

  @Override
  @Transactional
  public MessageResponse removeAdminRole(String username, UserPrincipal currentAccount) {
    Account account = accountRepository.getAccountByUsername(username);
    Collection<Role> roles = account.getRoles();
    Role adminRole = roleRepository.getRoleByRoleName(RoleName.ROLE_ADMIN);
    roles.remove(adminRole);
    account.setRoles(roles);
    Account updatedAccount = accountRepository.save(account);
    logger.info(
        "Account with id [{}] was taken ADMIN permission.", kv(ACCOUNT_ID, updatedAccount.getId()));
    return new MessageResponse(
        "Account with id [%d] was taken ADMIN permission.".formatted(updatedAccount.getId()));
  }
}
