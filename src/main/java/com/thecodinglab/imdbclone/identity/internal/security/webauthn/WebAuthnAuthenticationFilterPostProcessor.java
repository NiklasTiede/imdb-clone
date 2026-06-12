package com.thecodinglab.imdbclone.identity.internal.security.webauthn;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsFilter;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component
public class WebAuthnAuthenticationFilterPostProcessor implements BeanPostProcessor {

  private final ObjectProvider<WebAuthnLoginSuccessHandler> successHandler;
  private final ObjectProvider<SessionPublicKeyCredentialRequestOptionsRepository>
      requestOptionsRepository;

  public WebAuthnAuthenticationFilterPostProcessor(
      ObjectProvider<WebAuthnLoginSuccessHandler> successHandler,
      ObjectProvider<SessionPublicKeyCredentialRequestOptionsRepository> requestOptionsRepository) {
    this.successHandler = successHandler;
    this.requestOptionsRepository = requestOptionsRepository;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    if (bean instanceof WebAuthnAuthenticationFilter filter) {
      filter.setAuthenticationSuccessHandler(successHandler.getObject());
      filter.setRequestOptionsRepository(requestOptionsRepository.getObject());
    }
    if (bean instanceof PublicKeyCredentialRequestOptionsFilter filter) {
      filter.setRequestOptionsRepository(requestOptionsRepository.getObject());
    }
    return bean;
  }
}
