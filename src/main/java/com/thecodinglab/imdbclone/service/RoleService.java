package com.thecodinglab.imdbclone.service;

import com.thecodinglab.imdbclone.entity.Role;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface RoleService {

  List<Role> giveRoleToRegisteredUser();

  MessageResponse giveAdminRole(String username, UserPrincipal currentAccount);

  MessageResponse removeAdminRole(String username, UserPrincipal currentAccount);
}
