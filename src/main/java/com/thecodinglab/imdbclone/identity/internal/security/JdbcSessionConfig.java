package com.thecodinglab.imdbclone.identity.internal.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@Configuration
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 1_209_600)
public class JdbcSessionConfig {}
