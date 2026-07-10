package com.thecodinglab.imdbclone.identity.internal.security.webauthn;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component
public class WebAuthnAuthenticationFilterPostProcessor implements BeanPostProcessor {

  private final ObjectProvider<WebAuthnLoginSuccessHandler> successHandler;

  public WebAuthnAuthenticationFilterPostProcessor(
      ObjectProvider<WebAuthnLoginSuccessHandler> successHandler) {
    this.successHandler = successHandler;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    if (bean instanceof WebAuthnAuthenticationFilter filter) {
      filter.setAuthenticationSuccessHandler(successHandler.getObject());
    }
    return bean;
  }
}
