package com.thecodinglab.imdbclone.identity.internal.security.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "imdb-clone.identity.rate-limit")
public class RateLimitProperties {

  private boolean enabled = true;
  private Rule login = Rule.perMinute(10);
  private Rule passkeyLogin = Rule.perMinute(10);
  private Rule registration = Rule.perHour(5);
  private Rule passwordReset = Rule.perHour(5);
  private Rule oauth2Authorization = Rule.perMinute(20);
  private Rule loginFailure = Rule.perMinute(5);

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Rule getLogin() {
    return login;
  }

  public void setLogin(Rule login) {
    this.login = login;
  }

  public Rule getPasskeyLogin() {
    return passkeyLogin;
  }

  public void setPasskeyLogin(Rule passkeyLogin) {
    this.passkeyLogin = passkeyLogin;
  }

  public Rule getRegistration() {
    return registration;
  }

  public void setRegistration(Rule registration) {
    this.registration = registration;
  }

  public Rule getPasswordReset() {
    return passwordReset;
  }

  public void setPasswordReset(Rule passwordReset) {
    this.passwordReset = passwordReset;
  }

  public Rule getOauth2Authorization() {
    return oauth2Authorization;
  }

  public void setOauth2Authorization(Rule oauth2Authorization) {
    this.oauth2Authorization = oauth2Authorization;
  }

  public Rule getLoginFailure() {
    return loginFailure;
  }

  public void setLoginFailure(Rule loginFailure) {
    this.loginFailure = loginFailure;
  }

  public static class Rule {
    private long capacity;
    private Duration refillPeriod;

    public static Rule perMinute(long capacity) {
      return new Rule(capacity, Duration.ofMinutes(1));
    }

    public static Rule perHour(long capacity) {
      return new Rule(capacity, Duration.ofHours(1));
    }

    public Rule() {}

    private Rule(long capacity, Duration refillPeriod) {
      this.capacity = capacity;
      this.refillPeriod = refillPeriod;
    }

    public long getCapacity() {
      return capacity;
    }

    public void setCapacity(long capacity) {
      this.capacity = capacity;
    }

    public Duration getRefillPeriod() {
      return refillPeriod;
    }

    public void setRefillPeriod(Duration refillPeriod) {
      this.refillPeriod = refillPeriod;
    }
  }
}
