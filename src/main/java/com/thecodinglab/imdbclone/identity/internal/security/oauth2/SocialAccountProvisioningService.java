package com.thecodinglab.imdbclone.identity.internal.security.oauth2;

import com.thecodinglab.imdbclone.account.api.AccountIdentity;
import com.thecodinglab.imdbclone.account.api.AccountIdentityProviderLink;
import com.thecodinglab.imdbclone.account.api.AccountIdentityService;
import com.thecodinglab.imdbclone.identity.internal.security.CustomUserDetailsService;
import com.thecodinglab.imdbclone.identity.internal.security.audit.SecurityAuditEventType;
import com.thecodinglab.imdbclone.identity.internal.security.audit.SecurityAuditEvents;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import jakarta.transaction.Transactional;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class SocialAccountProvisioningService {

  private final AccountIdentityService accountIdentityService;
  private final CustomUserDetailsService userDetailsService;
  private final SecurityAuditEvents auditEvents;

  public SocialAccountProvisioningService(
      AccountIdentityService accountIdentityService,
      CustomUserDetailsService userDetailsService,
      SecurityAuditEvents auditEvents) {
    this.accountIdentityService = accountIdentityService;
    this.userDetailsService = userDetailsService;
    this.auditEvents = auditEvents;
  }

  @Transactional
  public UserPrincipal provision(SocialProviderIdentity socialIdentity) {
    Optional<AccountIdentityProviderLink> existingLink =
        accountIdentityService.findProviderLink(
            socialIdentity.provider(), socialIdentity.providerUserId());
    if (existingLink.isPresent()) {
      return loadPrincipal(existingLink.get().accountId());
    }

    if (!socialIdentity.emailVerified() || socialIdentity.email() == null) {
      auditEvents.recordCredentialEvent(
          SecurityAuditEventType.SOCIAL_PROVIDER_LINK_FAILED,
          null,
          Map.of("provider", socialIdentity.provider()));
      throw new OAuth2AuthenticationException("social_email_not_verified");
    }

    AccountIdentity account =
        accountIdentityService
            .findOptionalByEmail(socialIdentity.email())
            .orElseGet(
                () ->
                    accountIdentityService.createSocialAccount(
                        uniqueUsername(socialIdentity.email()), socialIdentity.email()));

    accountIdentityService.linkProvider(
        account.id(),
        socialIdentity.provider(),
        socialIdentity.providerUserId(),
        socialIdentity.email());
    auditEvents.recordCredentialEvent(
        SecurityAuditEventType.SOCIAL_PROVIDER_LINKED,
        account.id(),
        Map.of("provider", socialIdentity.provider()));
    return loadPrincipal(account.id());
  }

  private String uniqueUsername(String email) {
    String localPart = email.substring(0, email.indexOf('@'));
    String candidate = localPart.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    if (candidate.isBlank()) {
      candidate = "user";
    }
    if (accountIdentityService.isUsernameAvailable(candidate)) {
      return candidate;
    }
    for (int suffix = 2; ; suffix++) {
      String suffixed = candidate + "_" + suffix;
      if (accountIdentityService.isUsernameAvailable(suffixed)) {
        return suffixed;
      }
    }
  }

  private UserPrincipal loadPrincipal(Long accountId) {
    UserDetails userDetails = userDetailsService.loadUserById(accountId);
    if (userDetails instanceof UserPrincipal userPrincipal) {
      return userPrincipal;
    }
    throw new OAuth2AuthenticationException("social_account_principal_not_supported");
  }
}
