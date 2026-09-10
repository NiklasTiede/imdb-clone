package com.thecodinglab.imdbclone.identity.web;

import static org.assertj.core.api.Assertions.*;

import com.thecodinglab.imdbclone.identity.internal.security.SessionConciergeDelegation;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.session.MapSessionRepository;

class ConciergeDelegationControllerTest {
  private final Instant now = Instant.parse("2026-09-09T12:00:00Z");
  private final ConciergeDelegationController controller =
      new ConciergeDelegationController(
          new SessionConciergeDelegation(
              new MapSessionRepository(new ConcurrentHashMap<>()),
              Clock.fixed(now, ZoneOffset.UTC)));
  private final UserPrincipal user =
      new UserPrincipal(
          7L,
          null,
          null,
          "fixture",
          null,
          null,
          false,
          true,
          List.of(new SimpleGrantedAuthority("ROLE_USER")));

  @Test
  void grantsAShortLivedNonCacheableCredentialForTheExistingSession() {
    var request = new MockHttpServletRequest();
    var session = new MockHttpSession();
    request.setSession(session);
    var response = controller.createConciergeDelegation(request, user);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().expiresAt()).isEqualTo(now.plusSeconds(300));
    assertThat(response.getBody().token()).isNotBlank();
    assertThat(request.getSession(false)).isSameAs(session);
  }

  @Test
  void missingSessionIsRejectedWithoutCreatingAnAnonymousSession() {
    var request = new MockHttpServletRequest();
    assertThatThrownBy(() -> controller.createConciergeDelegation(request, user))
        .isInstanceOf(AccessDeniedException.class);
    assertThat(request.getSession(false)).isNull();
  }
}
