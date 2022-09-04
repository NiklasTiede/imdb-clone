package com.example.demo.controller;

import com.example.demo.entity.Account;
import com.example.demo.repository.AccountRepository;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.UserPrincipal;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping(("/account"))
public class AccountController {

  @Autowired private AccountRepository accountRepository;

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(AccountController.class));

  // ------------- GET ----------------

  @GetMapping("/bla")
  //  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<String> doSomething(@CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>("", HttpStatus.CREATED);
  }

  @GetMapping("/get-by-username/{userName}")
  public ResponseEntity<Account> getAccountByUsername(@PathVariable String userName) {
    Account account = accountRepository.findByUsername(userName).orElseThrow();
    return new ResponseEntity<>(account, HttpStatus.OK);
  }

  //  @GetMapping("/substringSearch/{phoneSubstring}")
  //  public ResponseEntity<List<User>> getUsersomethingi(
  //      @PathVariable(value = "phoneSubstring") String phoneSubstring) {
  //    List<User> users = userRepository.findByPhoneIsContaining(phoneSubstring);
  //    LOGGER.info("Number of similar users by phone " + users.size());
  //    return new ResponseEntity<>(users, HttpStatus.OK);
  //  }

  //  @GetMapping("/user/{id}")
  //  public ResponseEntity<User> getUserById(@PathVariable(value = "id") Integer id) {
  //    User user =
  //        userRepository
  //            .findById(id)
  //            .orElseThrow(
  //                () ->
  //                    new NotFoundException("User with UserId [" + id + "] not found in
  // database."));
  //    return new ResponseEntity<>(user, HttpStatus.OK);
  //  }

  // query parameters --> return a list of items
  //  @GetMapping("/users")
  //  public ResponseEntity<List<User>> getUsers(
  //      @RequestParam(required = false) String username, String email) {
  //    if (username != null && !username.isEmpty()) {
  //      List<User> users = new ArrayList<>();
  //      User user = userRepository.findByUsername(username);
  //      users.add(user);
  //      return new ResponseEntity<>(users, HttpStatus.OK);
  //    } else if (email != null && !email.isEmpty()) {
  //      List<User> users = new ArrayList<>();
  //      User user = userRepository.findByEmail(email);
  //      users.add(user);
  //      return new ResponseEntity<>(users, HttpStatus.OK);
  //    }
  //
  //    try {
  //      List<User> users = new ArrayList<>(userRepository.findAll());
  //      return new ResponseEntity<>(users, HttpStatus.OK);
  //    } catch (Exception e) {
  //      return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
  //    }
  //  }

  // ------------- POST ----------------

  //  @PostMapping("/register")
  //  public ResponseEntity<User> addUser(@RequestBody User user) {
  //    System.out.println(user);
  //    if (user.getEmail() == null | user.getUsername() == null | user.getPassword() == null) {
  //      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  //    }
  //    User savedEntity = userRepository.save(user);
  //    URI location =
  //        ServletUriComponentsBuilder.fromCurrentRequest()
  //            .path("/{id}")
  //            .buildAndExpand(savedEntity.getId())
  //            .toUri();
  //    return ResponseEntity.status(HttpStatus.CREATED).location(location).body(savedEntity);
  //  }

  // ------------- PUT ----------------

  //  @PutMapping("updateEmail")
  //  public User updateEmail(@RequestParam(required = false) String oldEmail, String newEmail) {
  //    User user = userRepository.findByEmail(oldEmail);
  //    user.setEmail(newEmail);
  //    return userRepository.save(user);
  //  }
  //
  //  @PutMapping("/users/{id}")
  //  User replaceUser(@RequestBody User newUser, @PathVariable int id) {
  //    User existingUser =
  //        userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not in db"));
  //    existingUser.setUsername(newUser.getUsername());
  //    existingUser.setEmail(newUser.getEmail());
  //    existingUser.setPassword(newUser.getPassword());
  //    return userRepository.save(existingUser);
  //  }

  // ------------- PATCH ----------------

  // ------------- DELETE ----------------

  //  @DeleteMapping("deleteUserById/{id}")
  //  public String deleteUser(@PathVariable("id") long id) {
  //    accountRepository.deleteById(id);
  //    return "User was deleted";
  //  }

  //  @DeleteMapping("deleteUserByName/{username}")
  //  public String deleteUser(@PathVariable("username") String username) {
  //    System.out.println("username to be deleted: " + username);
  //      accountRepository.deleteUserEntityByUsername(username);
  //    return "User was deleted";
  //  }
}
