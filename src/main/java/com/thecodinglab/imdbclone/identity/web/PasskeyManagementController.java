package com.thecodinglab.imdbclone.identity.web;

import com.thecodinglab.imdbclone.identity.internal.security.webauthn.PasskeyManagementService;
import com.thecodinglab.imdbclone.shared.security.CurrentUser;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account/passkeys")
public class PasskeyManagementController {

  private final PasskeyManagementService passkeyManagementService;

  public PasskeyManagementController(PasskeyManagementService passkeyManagementService) {
    this.passkeyManagementService = passkeyManagementService;
  }

  @GetMapping
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<List<PasskeyCredentialResponse>> listPasskeys(
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(passkeyManagementService.listPasskeys(currentUser), HttpStatus.OK);
  }

  @DeleteMapping("/{credentialId}")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<Void> deletePasskey(
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser,
      @PathVariable String credentialId) {
    passkeyManagementService.deletePasskey(currentUser, credentialId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
