package com.thecodinglab.imdbclone.security;

import com.thecodinglab.imdbclone.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private CustomUserDetailsService customUserDetailsService;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    try {
      String jwt = getJwtFromRequest(request);
      validateAndSetAuthentication(jwt, request);
    } catch (AuthenticationException ex) {
      logger.warn("Authentication failed", ex);
    } catch (Exception ex) {
      logger.error("Unexpected exception occurred", ex);
    }

    filterChain.doFilter(request, response);
  }

  private String getJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

  private void validateAndSetAuthentication(String jwt, HttpServletRequest request) {
    if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
      Long userId = jwtTokenProvider.getUserIdFromJWT(jwt);
      UserDetails userDetails = customUserDetailsService.loadUserById(userId);
      setAuthentication(request, userDetails);
    }
  }

  private void setAuthentication(HttpServletRequest request, UserDetails userDetails) {
    UsernamePasswordAuthenticationToken authenticationToken =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
  }
}
