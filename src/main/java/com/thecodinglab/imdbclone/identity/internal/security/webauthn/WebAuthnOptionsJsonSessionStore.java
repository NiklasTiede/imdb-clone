package com.thecodinglab.imdbclone.identity.internal.security.webauthn;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.Nullable;
import org.springframework.security.web.webauthn.jackson.WebauthnJacksonModule;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
class WebAuthnOptionsJsonSessionStore {

  private final JsonMapper jsonMapper =
      JsonMapper.builder().addModule(new WebauthnJacksonModule()).build();

  void save(HttpServletRequest request, String attributeName, @Nullable Object options) {
    HttpSession session = request.getSession();
    if (options == null) {
      session.removeAttribute(attributeName);
      return;
    }

    try {
      session.setAttribute(attributeName, jsonMapper.writeValueAsString(options));
    } catch (JacksonException ex) {
      throw new IllegalStateException("Could not serialize WebAuthn options", ex);
    }
  }

  <T> @Nullable T load(HttpServletRequest request, String attributeName, Class<T> optionsType) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return null;
    }

    Object serialized = session.getAttribute(attributeName);
    if (!(serialized instanceof String json)) {
      return null;
    }

    try {
      return jsonMapper.readValue(json, optionsType);
    } catch (JacksonException ex) {
      throw new IllegalStateException("Could not deserialize WebAuthn options", ex);
    }
  }
}
