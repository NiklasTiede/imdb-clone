package com.thecodinglab.imdbclone.identity.internal.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

class AccountLinkingOAuth2UserServiceTest {

  @Test
  void providerUserId_acceptsNumericGitHubIds() {
    var user =
        new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            Map.of("id", 12345, "login", "octocat"),
            "login");

    assertThat(AccountLinkingOAuth2UserService.providerUserId(user)).isEqualTo("12345");
  }
}
