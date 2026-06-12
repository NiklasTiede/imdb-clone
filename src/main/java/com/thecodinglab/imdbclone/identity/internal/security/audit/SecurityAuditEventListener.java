package com.thecodinglab.imdbclone.identity.internal.security.audit;

import com.thecodinglab.imdbclone.identity.internal.security.ratelimit.ClientIpResolver;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class SecurityAuditEventListener {

  private final SecurityAuditEvents auditEvents;
  private final ClientIpResolver clientIpResolver;

  public SecurityAuditEventListener(
      SecurityAuditEvents auditEvents, ClientIpResolver clientIpResolver) {
    this.auditEvents = auditEvents;
    this.clientIpResolver = clientIpResolver;
  }

  @EventListener
  public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
    Authentication authentication = event.getAuthentication();
    SecurityAuditEventType type = successType(authentication);
    auditEvents.recordAuthenticationEvent(
        type,
        authentication.getName(),
        accountId(authentication),
        currentIpAddress(),
        details(authentication));
  }

  @EventListener
  public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
    Authentication authentication = event.getAuthentication();
    SecurityAuditEventType type = failureType(authentication);
    auditEvents.recordAuthenticationEvent(
        type,
        authentication.getName(),
        accountId(authentication),
        currentIpAddress(),
        Map.of(
            "authenticationType",
            authentication.getClass().getSimpleName(),
            "exception",
            event.getException().getClass().getSimpleName()));
  }

  @EventListener
  public void onLogoutSuccess(LogoutSuccessEvent event) {
    Authentication authentication = event.getAuthentication();
    if (authentication == null) {
      return;
    }
    auditEvents.recordAuthenticationEvent(
        SecurityAuditEventType.LOGOUT_SUCCESS,
        authentication.getName(),
        accountId(authentication),
        currentIpAddress(),
        details(authentication));
  }

  private SecurityAuditEventType successType(Authentication authentication) {
    if (isOAuth2(authentication)) {
      return SecurityAuditEventType.OAUTH2_LOGIN_SUCCESS;
    }
    if (isWebAuthn(authentication)) {
      return SecurityAuditEventType.PASSKEY_LOGIN_SUCCESS;
    }
    return SecurityAuditEventType.PASSWORD_LOGIN_SUCCESS;
  }

  private SecurityAuditEventType failureType(Authentication authentication) {
    if (isOAuth2(authentication)) {
      return SecurityAuditEventType.OAUTH2_LOGIN_FAILURE;
    }
    if (isWebAuthn(authentication)) {
      return SecurityAuditEventType.PASSKEY_LOGIN_FAILURE;
    }
    return SecurityAuditEventType.PASSWORD_LOGIN_FAILURE;
  }

  private boolean isWebAuthn(Authentication authentication) {
    return authentication.getClass().getName().contains("webauthn")
        || authentication.getClass().getSimpleName().contains("WebAuthn");
  }

  private boolean isOAuth2(Authentication authentication) {
    return authentication instanceof OAuth2AuthenticationToken
        || authentication
            .getClass()
            .getName()
            .toLowerCase(java.util.Locale.ROOT)
            .contains("oauth2");
  }

  private Map<String, Object> details(Authentication authentication) {
    if (authentication instanceof OAuth2AuthenticationToken oauth2) {
      return Map.of(
          "authenticationType",
          authentication.getClass().getSimpleName(),
          "provider",
          oauth2.getAuthorizedClientRegistrationId());
    }
    return Map.of("authenticationType", authentication.getClass().getSimpleName());
  }

  private Long accountId(Authentication authentication) {
    if (authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
      return userPrincipal.getId();
    }
    return null;
  }

  private String currentIpAddress() {
    if (RequestContextHolder.getRequestAttributes()
        instanceof ServletRequestAttributes attributes) {
      HttpServletRequest request = attributes.getRequest();
      return clientIpResolver.resolve(request);
    }
    return null;
  }
}
