package com.thecodinglab.imdbclone.account.api;

import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;

public interface RoleService {

  MessageResponse giveAdminRole(String username, UserPrincipal currentAccount);

  MessageResponse removeAdminRole(String username, UserPrincipal currentAccount);
}
