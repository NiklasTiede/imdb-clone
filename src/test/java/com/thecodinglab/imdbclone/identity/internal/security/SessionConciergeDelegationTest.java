package com.thecodinglab.imdbclone.identity.internal.security;

import static org.assertj.core.api.Assertions.*;

import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.MapSession;
import org.springframework.session.MapSessionRepository;

class SessionConciergeDelegationTest {
  private final Clock clock = Clock.fixed(Instant.parse("2026-09-09T12:00:00Z"), ZoneOffset.UTC);
  private final MapSessionRepository sessions = new MapSessionRepository(new ConcurrentHashMap<>());
  private final SessionConciergeDelegation service =
      new SessionConciergeDelegation(sessions, clock);

  @Test
  void verifiesOnlyTheSignedSessionActorAndAllowedScope() {
    var fixture = fixture(7L);
    var actor = service.verify(fixture.token(), "watchlist:add");
    assertThat(actor.accountId()).isEqualTo(7L);
    for (String scope : List.of("watchlist:remove", "ratings:set", "ratings:remove"))
      assertThat(service.verify(fixture.token(), scope)).isEqualTo(actor);
    assertThat(actor.sessionBinding())
        .isNotEqualTo(service.verify(fixture(8L).token(), "watchlist:read").sessionBinding());
    assertThatThrownBy(() -> service.verify(fixture.token(), "account:delete"))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> service.verify(fixture.token() + "x", "watchlist:read"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void logoutAndExpiryRevokeEvenAnOtherwiseValidCredential() {
    var fixture = fixture(7L);
    var later =
        new SessionConciergeDelegation(sessions, Clock.offset(clock, Duration.ofSeconds(301)));
    assertThatThrownBy(() -> later.verify(fixture.token(), "watchlist:read"))
        .isInstanceOf(AccessDeniedException.class);
    sessions.deleteById(fixture.session().getId());
    assertThatThrownBy(() -> service.verify(fixture.token(), "watchlist:add"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void changedPrincipalCannotReuseThePreviousAccountsGrant() {
    var fixture = fixture(7L);
    var session = sessions.findById(fixture.session().getId());
    session.setAttribute("SPRING_SECURITY_CONTEXT", context(8L));
    sessions.save(session);
    assertThatThrownBy(() -> service.verify(fixture.token(), "watchlist:read"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void malformedAndAnonymousCredentialsFailClosed() {
    for (String token : List.of("", "garbage", "abc.def", "a.b.c")) {
      assertThatThrownBy(() -> service.verify(token, "watchlist:read"))
          .isInstanceOf(AccessDeniedException.class);
    }
    assertThatThrownBy(() -> service.verify(null, "watchlist:read"))
        .isInstanceOf(AccessDeniedException.class);
  }

  private Fixture fixture(Long id) {
    var http = new MockHttpSession();
    var token = service.issue(http, user(id)).token();
    var stored = new MapSession(http.getId());
    stored.setAttribute("CONCIERGE_DELEGATION_KEY", http.getAttribute("CONCIERGE_DELEGATION_KEY"));
    stored.setAttribute("SPRING_SECURITY_CONTEXT", context(id));
    sessions.save(stored);
    return new Fixture(stored, token);
  }

  private org.springframework.security.core.context.SecurityContext context(Long id) {
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            user(id), null, user(id).getAuthorities()));
    return context;
  }

  private UserPrincipal user(Long id) {
    return new UserPrincipal(
        id,
        null,
        null,
        "fixture",
        null,
        null,
        false,
        true,
        List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }

  private record Fixture(MapSession session, String token) {}
}
