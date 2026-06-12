package com.thecodinglab.imdbclone.identity.internal.security.webauthn;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository;
import org.springframework.stereotype.Component;

@Component
public class SessionPublicKeyCredentialRequestOptionsRepository
    implements PublicKeyCredentialRequestOptionsRepository {

  private static final String ATTRIBUTE_NAME =
      SessionPublicKeyCredentialRequestOptionsRepository.class.getName() + ".OPTIONS";

  private final WebAuthnOptionsJsonSessionStore sessionStore;

  public SessionPublicKeyCredentialRequestOptionsRepository(
      WebAuthnOptionsJsonSessionStore sessionStore) {
    this.sessionStore = sessionStore;
  }

  @Override
  public void save(
      HttpServletRequest request,
      HttpServletResponse response,
      @Nullable PublicKeyCredentialRequestOptions options) {
    sessionStore.save(request, ATTRIBUTE_NAME, options);
  }

  @Override
  public @Nullable PublicKeyCredentialRequestOptions load(HttpServletRequest request) {
    return sessionStore.load(request, ATTRIBUTE_NAME, PublicKeyCredentialRequestOptions.class);
  }
}
