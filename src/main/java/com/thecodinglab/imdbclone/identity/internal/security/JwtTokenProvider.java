package com.thecodinglab.imdbclone.identity.internal.security;

import com.thecodinglab.imdbclone.identity.internal.IdentityProperties;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

  private final IdentityProperties identityProperties;

  public JwtTokenProvider(IdentityProperties identityProperties) {
    this.identityProperties = identityProperties;
  }

  public String generateToken(Authentication authentication) {
    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
    SecretKey key = createSigningKey();
    Date expiryDate = new Date((new Date()).getTime() + identityProperties.jwt().expirationInMs());

    return Jwts.builder()
        .claims(createClaims(userPrincipal))
        .subject(Long.toString(userPrincipal.getId()))
        .issuedAt(new Date())
        .expiration(expiryDate)
        .signWith(key, Jwts.SIG.HS512)
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
    return Jwts.parser()
        .verifyWith(createSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private SecretKey createSigningKey() {
    return new SecretKeySpec(
        Base64.getDecoder().decode(identityProperties.jwt().secret()), "HmacSHA512");
  }
}
