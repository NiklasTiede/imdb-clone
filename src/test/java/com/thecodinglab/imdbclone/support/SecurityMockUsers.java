package com.thecodinglab.imdbclone.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class SecurityMockUsers {

  private SecurityMockUsers() {}

  public static RequestPostProcessor testAdmin() {
    return user(
        new UserPrincipal(
            1L,
            "Test",
            "Admin",
            "test_user_one",
            "one@gmail.com",
            "",
            false,
            true,
            List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER"))));
  }

  public static RequestPostProcessor testUser() {
    return user(
        new UserPrincipal(
            2L,
            "Test",
            "User",
            "test_user_two",
            "two@web.com",
            "",
            false,
            true,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))));
  }
}
