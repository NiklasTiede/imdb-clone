package com.thecodinglab.imdbclone.account.internal.persistence;

import com.thecodinglab.imdbclone.shared.error.NotFoundException;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findByName(RoleName roleName);

  default Role getRoleByRoleName(RoleName roleName) {
    return findByName(roleName)
        .orElseThrow(
            () -> new NotFoundException("Role with roleName [" + roleName + "] not found."));
  }
}
