package com.example.demo.repository;

import com.example.demo.entity.Role;
import com.example.demo.enums.RoleNameEnum;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(RoleNameEnum roleNameEnum);
}
