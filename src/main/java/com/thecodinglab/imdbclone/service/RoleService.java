package com.thecodinglab.imdbclone.service;

import com.thecodinglab.imdbclone.entity.Role;
import com.thecodinglab.imdbclone.identity.api.UserPrincipal;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import java.util.List;

public interface RoleService {

  List<Role> giveRoleToRegisteredUser();

  MessageResponse giveAdminRole(String username, UserPrincipal currentAccount);

  MessageResponse removeAdminRole(String username, UserPrincipal currentAccount);
}
