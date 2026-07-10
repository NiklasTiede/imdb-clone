package com.thecodinglab.imdbclone.identity.internal.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

final class CsrfTokenRefreshingLogoutSuccessHandler implements LogoutSuccessHandler {

  private final CsrfTokenRepository csrfTokenRepository;

  CsrfTokenRefreshingLogoutSuccessHandler(CsrfTokenRepository csrfTokenRepository) {
    this.csrfTokenRepository = csrfTokenRepository;
  }

  @Override
  public void onLogoutSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
    csrfTokenRepository.saveToken(csrfToken, request, response);
    response.setStatus(HttpStatus.OK.value());
  }
}
