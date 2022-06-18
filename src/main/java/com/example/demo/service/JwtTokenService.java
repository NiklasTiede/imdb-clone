package com.example.demo.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {

  private final String secret;

  private final Long expiration;

  public JwtTokenService(
      @Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") Long expiration) {
    this.secret = secret;
    this.expiration = expiration;
  }

  public String generateToken(String username) {
    final Date createdDate = new Date();
    final Date expirationDate = calculateExpirationDate(createdDate);

    return Jwts.builder()
        .setClaims(new HashMap<>())
        .setSubject(username)
        .setIssuedAt(createdDate)
        .setExpiration(expirationDate)
        .signWith(SignatureAlgorithm.HS512, secret)
        .compact();
  }

  private Date calculateExpirationDate(Date createdDate) {
    return new Date(createdDate.getTime() + expiration * 10000);
  }
}
