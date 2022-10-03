package com.example.demo.repository;

import com.example.demo.entity.Role;
import com.example.demo.enums.RoleNameEnum;
import com.example.demo.exceptions.NotFoundException;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findByName(RoleNameEnum roleNameEnum);

  default Role getRoleByRoleEnum(RoleNameEnum roleNameEnum) {
    return findByName(roleNameEnum)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Role with roleNameEnum [" + roleNameEnum + "] not found in database."));
  }
}
