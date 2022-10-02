package com.example.demo.service;

import com.example.demo.entity.Role;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface RoleService {

  List<Role> giveRoleToRegisteredUser();
}
