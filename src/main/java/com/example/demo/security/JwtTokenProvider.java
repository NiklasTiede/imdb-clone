package com.example.demo.security;

import io.jsonwebtoken.*;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenProvider.class);

  @Value(value = "${app.jwtSecret}")
  private String jwtSecret;

  @Value(value = "${app.jwtExpirationInMs}")
  private int jwtExpirationInMs;

  public String generateToken(Authentication authentication) {
    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

    Key key =
        new SecretKeySpec(
            Base64.getDecoder().decode(jwtSecret), SignatureAlgorithm.HS512.getJcaName());

    return Jwts.builder()
        .setSubject(Long.toString(userPrincipal.getId()))
        .setIssuedAt(new Date())
        .setExpiration(expiryDate)
        .signWith(key)
        .compact();
  }

  public Long getUserIdFromJWT(String token) {

    // refactor from both methods
    Key key =
        new SecretKeySpec(
            Base64.getDecoder().decode(jwtSecret), SignatureAlgorithm.HS512.getJcaName());

    Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    //    Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();

    return Long.valueOf(claims.getSubject());
  }

  public boolean validateToken(String authToken) {

    Key key =
        new SecretKeySpec(
            Base64.getDecoder().decode(jwtSecret), SignatureAlgorithm.HS512.getJcaName());

    try {
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
      //      Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
      return true;
      // ExpiredJwtException, UnsupportedJwtException, MalformedJwtException,
      // io.jsonwebtoken.security.SignatureException, IllegalArgumentException
    } catch (SecurityException ex) {
      LOGGER.error("Invalid JWT signature");
    } catch (MalformedJwtException ex) {
      LOGGER.error("Invalid JWT token");
    } catch (ExpiredJwtException ex) {
      LOGGER.error("Expired JWT token");
    } catch (UnsupportedJwtException ex) {
      LOGGER.error("Unsupported JWT token");
    } catch (IllegalArgumentException ex) {
      LOGGER.error("JWT claims string is empty");
    }
    return false;
  }
}
