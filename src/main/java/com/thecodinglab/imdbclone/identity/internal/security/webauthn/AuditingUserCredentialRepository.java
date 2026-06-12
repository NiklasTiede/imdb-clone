package com.thecodinglab.imdbclone.identity.internal.security.webauthn;

import com.thecodinglab.imdbclone.identity.internal.security.audit.SecurityAuditEventType;
import com.thecodinglab.imdbclone.identity.internal.security.audit.SecurityAuditEvents;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;

public class AuditingUserCredentialRepository implements UserCredentialRepository {

  private final UserCredentialRepository delegate;
  private final SecurityAuditEvents auditEvents;

  public AuditingUserCredentialRepository(
      UserCredentialRepository delegate, SecurityAuditEvents auditEvents) {
    this.delegate = delegate;
    this.auditEvents = auditEvents;
  }

  @Override
  public void delete(Bytes credentialId) {
    delegate.delete(credentialId);
  }

  @Override
  public void save(CredentialRecord record) {
    boolean newCredential = delegate.findByCredentialId(record.getCredentialId()) == null;
    delegate.save(record);

    if (newCredential) {
      recordPasskeyRegistered(record);
    }
  }

  @Override
  public @Nullable CredentialRecord findByCredentialId(Bytes credentialId) {
    return delegate.findByCredentialId(credentialId);
  }

  @Override
  public List<CredentialRecord> findByUserId(Bytes userId) {
    return delegate.findByUserId(userId);
  }

  private void recordPasskeyRegistered(CredentialRecord record) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication != null
        && authentication.getPrincipal() instanceof UserPrincipal currentUser)) {
      return;
    }

    Map<String, Object> details = new HashMap<>();
    details.put("credentialId", record.getCredentialId().toBase64UrlString());
    if (record.getLabel() != null) {
      details.put("label", record.getLabel());
    }
    auditEvents.recordCredentialEvent(
        SecurityAuditEventType.PASSKEY_REGISTERED, currentUser.getId(), details);
  }
}
