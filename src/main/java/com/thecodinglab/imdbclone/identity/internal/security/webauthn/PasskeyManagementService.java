package com.thecodinglab.imdbclone.identity.internal.security.webauthn;

import com.thecodinglab.imdbclone.identity.internal.security.audit.SecurityAuditEventType;
import com.thecodinglab.imdbclone.identity.internal.security.audit.SecurityAuditEvents;
import com.thecodinglab.imdbclone.identity.web.PasskeyCredentialResponse;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasskeyManagementService {

  private final JdbcOperations jdbcOperations;
  private final SecurityAuditEvents auditEvents;

  public PasskeyManagementService(JdbcOperations jdbcOperations, SecurityAuditEvents auditEvents) {
    this.jdbcOperations = jdbcOperations;
    this.auditEvents = auditEvents;
  }

  @Transactional(readOnly = true)
  public List<PasskeyCredentialResponse> listPasskeys(UserPrincipal currentUser) {
    return jdbcOperations.query(
        """
        select c.credential_id, c.label, c.created, c.last_used
        from user_credentials c
        join user_entities u on u.id = c.user_entity_user_id
        where u.name = ?
        order by c.created desc
        """,
        (rs, rowNum) -> toResponse(rs),
        currentUser.getUsername());
  }

  @Transactional
  public void deletePasskey(UserPrincipal currentUser, String credentialId) {
    int deletedRows =
        jdbcOperations.update(
            """
            delete from user_credentials c
            using user_entities u
            where c.user_entity_user_id = u.id
              and u.name = ?
              and c.credential_id = ?
            """,
            currentUser.getUsername(),
            credentialId);

    if (deletedRows == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    auditEvents.recordCredentialEvent(
        SecurityAuditEventType.PASSKEY_DELETED,
        currentUser.getId(),
        Map.of("credentialId", credentialId));
  }

  private PasskeyCredentialResponse toResponse(ResultSet rs) throws SQLException {
    return new PasskeyCredentialResponse(
        rs.getString("credential_id"),
        rs.getString("label"),
        rs.getTimestamp("created").toInstant(),
        rs.getTimestamp("last_used").toInstant());
  }
}
