package com.thecodinglab.imdbclone.identity.internal.security.oauth2;

import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class AccountLinkingOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

  private final OidcUserService delegate = new OidcUserService();
  private final SocialAccountProvisioningService provisioningService;

  public AccountLinkingOidcUserService(SocialAccountProvisioningService provisioningService) {
    this.provisioningService = provisioningService;
  }

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUser oidcUser = delegate.loadUser(userRequest);
    UserPrincipal principal =
        provisioningService.provision(
            new SocialProviderIdentity(
                userRequest.getClientRegistration().getRegistrationId(),
                oidcUser.getSubject(),
                oidcUser.getEmail(),
                Boolean.TRUE.equals(oidcUser.getEmailVerified()),
                oidcUser.getClaims()));
    return SocialUserPrincipal.from(principal, oidcUser);
  }
}
