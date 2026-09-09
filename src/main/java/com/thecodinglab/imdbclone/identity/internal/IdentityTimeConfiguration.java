package com.thecodinglab.imdbclone.identity.internal;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class IdentityTimeConfiguration {

  @Bean
  Clock identityClock() {
    return Clock.systemUTC();
  }
}
