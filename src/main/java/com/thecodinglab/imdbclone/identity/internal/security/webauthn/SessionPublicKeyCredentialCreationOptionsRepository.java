package com.thecodinglab.imdbclone.identity.internal.security.webauthn;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.registration.PublicKeyCredentialCreationOptionsRepository;
import org.springframework.stereotype.Component;

@Component
public class SessionPublicKeyCredentialCreationOptionsRepository
    implements PublicKeyCredentialCreationOptionsRepository {

  private static final String ATTRIBUTE_NAME =
      SessionPublicKeyCredentialCreationOptionsRepository.class.getName() + ".OPTIONS";

  private final WebAuthnOptionsJsonSessionStore sessionStore;

  public SessionPublicKeyCredentialCreationOptionsRepository(
      WebAuthnOptionsJsonSessionStore sessionStore) {
    this.sessionStore = sessionStore;
  }

  @Override
  public void save(
      HttpServletRequest request,
      HttpServletResponse response,
      @Nullable PublicKeyCredentialCreationOptions options) {
    sessionStore.save(request, ATTRIBUTE_NAME, options);
  }

  @Override
  public @Nullable PublicKeyCredentialCreationOptions load(HttpServletRequest request) {
    return sessionStore.load(request, ATTRIBUTE_NAME, PublicKeyCredentialCreationOptions.class);
  }
}
