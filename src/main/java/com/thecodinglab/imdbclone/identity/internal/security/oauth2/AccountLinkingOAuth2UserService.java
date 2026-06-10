package com.thecodinglab.imdbclone.identity.internal.security.oauth2;

import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AccountLinkingOAuth2UserService
    implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private static final ParameterizedTypeReference<List<GitHubEmail>> GITHUB_EMAILS_TYPE =
      new ParameterizedTypeReference<>() {};

  private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
  private final RestClient restClient;
  private final SocialAccountProvisioningService provisioningService;

  public AccountLinkingOAuth2UserService(SocialAccountProvisioningService provisioningService) {
    this.restClient = RestClient.builder().build();
    this.provisioningService = provisioningService;
  }

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oauth2User = delegate.loadUser(userRequest);
    String provider = userRequest.getClientRegistration().getRegistrationId();
    String providerUserId = providerUserId(oauth2User);
    VerifiedEmail verifiedEmail = verifiedEmail(provider, userRequest, oauth2User);
    UserPrincipal principal =
        provisioningService.provision(
            new SocialProviderIdentity(
                provider,
                providerUserId,
                verifiedEmail.email(),
                verifiedEmail.verified(),
                oauth2User.getAttributes()));
    return SocialUserPrincipal.from(principal, oauth2User);
  }

  private VerifiedEmail verifiedEmail(
      String provider, OAuth2UserRequest userRequest, OAuth2User oauth2User) {
    if ("github".equals(provider)) {
      return verifiedGitHubEmail(userRequest);
    }
    String email = oauth2User.getAttribute("email");
    return new VerifiedEmail(email, email != null);
  }

  private VerifiedEmail verifiedGitHubEmail(OAuth2UserRequest userRequest) {
    List<GitHubEmail> emails =
        restClient
            .get()
            .uri("https://api.github.com/user/emails")
            .headers(headers -> headers.setBearerAuth(userRequest.getAccessToken().getTokenValue()))
            .retrieve()
            .body(GITHUB_EMAILS_TYPE);
    if (emails == null) {
      return new VerifiedEmail(null, false);
    }
    return emails.stream()
        .filter(email -> Boolean.TRUE.equals(email.primary()))
        .filter(email -> Boolean.TRUE.equals(email.verified()))
        .findFirst()
        .map(email -> new VerifiedEmail(email.email(), true))
        .orElseGet(() -> new VerifiedEmail(null, false));
  }

  static String providerUserId(OAuth2User oauth2User) {
    Object providerUserId = oauth2User.getAttributes().get("id");
    if (providerUserId == null) {
      return oauth2User.getName();
    }
    return providerUserId.toString();
  }

  private record VerifiedEmail(String email, boolean verified) {}

  private record GitHubEmail(String email, Boolean primary, Boolean verified) {}
}
