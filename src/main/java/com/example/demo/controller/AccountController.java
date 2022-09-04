package com.example.demo.controller;

import com.example.demo.Payload.MessageResponse;
import com.example.demo.entity.Account;
import com.example.demo.repository.AccountRepository;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.UserPrincipal;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping(("/account"))
public class AccountController {

  @Autowired private AccountRepository accountRepository;

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(AccountController.class));

  @GetMapping("/me")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> getMe(@CurrentUser UserPrincipal currentUser) {

    return new ResponseEntity<>(new MessageResponse("answer"), HttpStatus.CREATED);
  }

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

  //    @PostMapping("/register")
  //    public ResponseEntity<User> addUser(@RequestBody User user) {
  //      System.out.println(user);
  //      if (user.getEmail() == null | user.getUsername() == null | user.getPassword() == null) {
  //        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  //      }
  //      User savedEntity = userRepository.save(user);
  //      URI location =
  //          ServletUriComponentsBuilder.fromCurrentRequest()
  //              .path("/{id}")
  //              .buildAndExpand(savedEntity.getId())
  //              .toUri();
  //      return ResponseEntity.status(HttpStatus.CREATED).location(location).body(savedEntity);
  //    }
}
