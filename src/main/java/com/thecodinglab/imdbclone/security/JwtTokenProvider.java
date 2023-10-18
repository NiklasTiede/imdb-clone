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

    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

    Key key =
        new SecretKeySpec(
            Base64.getDecoder().decode(jwtSecret), SignatureAlgorithm.HS512.getJcaName());

    List<String> currentUserRoles =
        userPrincipal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    return Jwts.builder()
        .setClaims(Map.of("roles", currentUserRoles, "username", userPrincipal.getUsername()))
        .setSubject(Long.toString(userPrincipal.getId()))
        .setIssuedAt(new Date())
        .setExpiration(expiryDate)
        .signWith(key)
        .compact();
  }

  public Long getUserIdFromJWT(String token) {

    Key key =
        new SecretKeySpec(
            Base64.getDecoder().decode(jwtSecret), SignatureAlgorithm.HS512.getJcaName());

    Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

    return Long.valueOf(claims.getSubject());
  }

  public boolean validateToken(String authToken) {

    Key key =
        new SecretKeySpec(
            Base64.getDecoder().decode(jwtSecret), SignatureAlgorithm.HS512.getJcaName());

    try {
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
      return true;
    } catch (SecurityException ex) {
      logger.error("Invalid JWT signature");
    } catch (MalformedJwtException ex) {
      logger.error("Invalid JWT token");
    } catch (ExpiredJwtException ex) {
      logger.error("Expired JWT token");
    } catch (UnsupportedJwtException ex) {
      logger.error("Unsupported JWT token");
    } catch (IllegalArgumentException ex) {
      logger.error("JWT claims string is empty");
    }
    return false;
  }
}
