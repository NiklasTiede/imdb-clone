package com.thecodinglab.imdbclone.identity.internal.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

  private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();
  private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
    xor.handle(request, response, csrfToken);
    if (csrfToken.get() == null) {
      throw new IllegalStateException("Deferred CSRF token was not available");
    }
  }

  @Override
  public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
    String headerValue = request.getHeader(csrfToken.getHeaderName());
    return StringUtils.hasText(headerValue)
        ? plain.resolveCsrfTokenValue(request, csrfToken)
        : xor.resolveCsrfTokenValue(request, csrfToken);
  }
}
