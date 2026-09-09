package com.thecodinglab.imdbclone.identity.web;

import com.thecodinglab.imdbclone.identity.api.ConciergeDelegationResponse;
import com.thecodinglab.imdbclone.identity.internal.security.SessionConciergeDelegation;
import com.thecodinglab.imdbclone.shared.security.CurrentUser;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/v{version}/auth/concierge-delegation", version = "1")
public class ConciergeDelegationController {
  private final SessionConciergeDelegation delegation;

  public ConciergeDelegationController(SessionConciergeDelegation delegation) {
    this.delegation = delegation;
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public ResponseEntity<ConciergeDelegationResponse> createConciergeDelegation(
      HttpServletRequest request, @Parameter(hidden = true) @CurrentUser UserPrincipal user) {
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(delegation.issue(request.getSession(false), user));
  }
}
