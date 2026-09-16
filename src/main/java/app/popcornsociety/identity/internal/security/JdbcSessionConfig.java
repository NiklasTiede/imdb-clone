package app.popcornsociety.identity.internal.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 1_209_600)
public class JdbcSessionConfig {
  static final String SESSION_COOKIE_NAME = "POPCORN_SESSION";

  @Bean
  CookieSerializer cookieSerializer() {
    // Old sessions contain serialized principals from the previous Java package.
    // A new cookie name makes the test accounts reauthenticate without loading them.
    var serializer = new DefaultCookieSerializer();
    serializer.setCookieName(SESSION_COOKIE_NAME);
    return serializer;
  }
}
