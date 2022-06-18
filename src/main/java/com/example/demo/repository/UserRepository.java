package com.example.demo.repository;

import com.example.demo.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// @Transactional
public interface UserRepository extends JpaRepository<User, Integer> { // CrudRepository

  User findByUsername(String username);

  Optional<User> findOneByUsername(String username);

  User findByEmail(String password);

  void deleteUserEntityByUsername(String username);

  List<User> findByPhoneIsContaining(String phone);
}
