package com.thecodinglab.imdbclone.identity.internal.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

class SocialUserPrincipalTest {

  @Test
  void from_ignoresNullProviderAttributes() {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("id", 12345);
    attributes.put("login", "octocat");
    attributes.put("email", null);
    var oauth2User =
        new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "login");

    SocialUserPrincipal principal = SocialUserPrincipal.from(accountPrincipal(), oauth2User);

    assertThat(principal.getAttributes())
        .containsEntry("id", 12345)
        .containsEntry("login", "octocat")
        .doesNotContainKey("email");
  }

  private UserPrincipal accountPrincipal() {
    return new UserPrincipal(
        1L,
        null,
        null,
        "octocat",
        "octocat@example.com",
        null,
        false,
        true,
        List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }
}
