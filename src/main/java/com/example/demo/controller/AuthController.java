package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.RegistrationRequest;
import com.example.demo.dto.RegistrationResponse;
import com.example.demo.entity.Account;
import com.example.demo.entity.role.Role;
import com.example.demo.entity.role.RoleName;
import com.example.demo.exceptions.BadRequestException;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.security.JwtTokenProvider;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private static final String USER_ROLE_NOT_SET = "User role not set";

  @Autowired private AuthenticationManager authenticationManager;

  @Autowired private AccountRepository accountRepository;

  @Autowired private RoleRepository roleRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtTokenProvider jwtTokenProvider;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> authenticateUser(
      @Valid @RequestBody LoginRequest loginRequest) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.usernameOrEmail(), loginRequest.password()));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    String jwt = jwtTokenProvider.generateToken(authentication);
    return ResponseEntity.ok(new LoginResponse(jwt));
  }

  @PostMapping("/registration")
  public ResponseEntity<RegistrationResponse> registerUser(
      @Valid @RequestBody RegistrationRequest signUpRequest) {
    System.out.println(signUpRequest);
    if (Boolean.TRUE.equals(accountRepository.existsByUsername(signUpRequest.username()))) {
      throw new BadRequestException("Username is already taken");
    }
    if (Boolean.TRUE.equals(accountRepository.existsByEmail(signUpRequest.email()))) {
      throw new BadRequestException("Email is already taken");
    }

    String firstName = signUpRequest.firstName().toLowerCase();
    String lastName = signUpRequest.lastName().toLowerCase();
    String username = signUpRequest.username().toLowerCase();
    String email = signUpRequest.email().toLowerCase();
    String password = passwordEncoder.encode(signUpRequest.password());

    Account account = new Account(firstName, lastName, username, email, password);
    List<Role> roles = new ArrayList<>();

    if (accountRepository.count() == 0) {
      roles.add(
          roleRepository
              .findByName(RoleName.ROLE_USER.name())
              .orElseThrow(() -> new NotFoundException(USER_ROLE_NOT_SET)));
      roles.add(
          roleRepository
              .findByName(RoleName.ROLE_ADMIN.name())
              .orElseThrow(() -> new NotFoundException(USER_ROLE_NOT_SET)));
    } else {
      roles.add(
          roleRepository
              .findByName(RoleName.ROLE_USER.name())
              .orElseThrow(() -> new NotFoundException(USER_ROLE_NOT_SET)));
    }
    System.out.println(roles);

    account.setRoles(roles);
    Account result = accountRepository.save(account);

    URI location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/users/{userId}")
            .buildAndExpand(result.getId())
            .toUri();

    return ResponseEntity.created(location)
        .body(new RegistrationResponse(Boolean.TRUE, "User registered successfully"));
  }
}
