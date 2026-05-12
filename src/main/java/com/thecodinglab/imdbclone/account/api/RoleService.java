package com.thecodinglab.imdbclone.account.api;

import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.security.UserPrincipal;

public interface RoleService {

  MessageResponse giveAdminRole(String username, UserPrincipal currentAccount);

  MessageResponse removeAdminRole(String username, UserPrincipal currentAccount);
}
