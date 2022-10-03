package com.example.demo.service;

import com.example.demo.entity.Role;
import com.example.demo.payload.MessageResponse;
import com.example.demo.security.UserPrincipal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface RoleService {

  List<Role> giveRoleToRegisteredUser();

  MessageResponse giveAdminRole(String username, UserPrincipal currentAccount);

  MessageResponse removeAdminRole(String username, UserPrincipal currentAccount);
}
