package com.thecodinglab.imdbclone.security;

import io.jsonwebtoken.*;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

  @Value(value = "${jwt.secret}")
  private String jwtSecret;

  @Value(value = "${jwt.expiration-in-ms}")
  private long jwtExpirationInMs;

  public String generateToken(Authentication authentication) {
    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
    Key key = createSigningKey();
    Date expiryDate = new Date((new Date()).getTime() + jwtExpirationInMs);

    return Jwts.builder()
        .setClaims(createClaims(userPrincipal))
        .setSubject(Long.toString(userPrincipal.getId()))
        .setIssuedAt(new Date())
        .setExpiration(expiryDate)
        .signWith(key)
        .compact();
  }

  private Map<String, Object> createClaims(UserPrincipal userPrincipal) {
    List<String> roles =
        userPrincipal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    return Map.of("roles", roles, "username", userPrincipal.getUsername());
  }

  public Long getUserIdFromJWT(String token) {
    Claims claims = extractClaims(token);
    return Long.valueOf(claims.getSubject());
  }

  public boolean validateToken(String authToken) {
    try {
      extractClaims(authToken);
      return true;
    } catch (JwtException ex) {
      logger.error("JWT validation failed: {}", ex.getMessage());
      return false;
    }
  }

  private Claims extractClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(createSigningKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  private Key createSigningKey() {
    return new SecretKeySpec(
        Base64.getDecoder().decode(jwtSecret), SignatureAlgorithm.HS512.getJcaName());
  }
}
