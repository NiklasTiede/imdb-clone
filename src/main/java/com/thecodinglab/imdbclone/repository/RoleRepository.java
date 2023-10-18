package com.thecodinglab.imdbclone.repository;

import com.thecodinglab.imdbclone.entity.Role;
import com.thecodinglab.imdbclone.enums.RoleNameEnum;
import com.thecodinglab.imdbclone.exception.domain.NotFoundException;
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
