package com.thecodinglab.imdbclone.identity;

import static com.thecodinglab.imdbclone.support.SecurityMockUsers.testUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecodinglab.imdbclone.support.BaseContainers;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "imdb-clone.identity.webauthn.rp-id=localhost",
      "imdb-clone.identity.webauthn.allowed-origins=http://localhost:3000"
    })
@AutoConfigureMockMvc
class PasskeyManagementControllerTest extends BaseContainers {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  private final String userEntityId = base64Url(1);
  private final String credentialId = base64Url(2);
  private final String otherUserCredentialId = base64Url(3);

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("delete from security_audit_event");
    jdbcTemplate.update("delete from user_credentials");
    jdbcTemplate.update("delete from user_entities");
  }

  @Test
  void currentAccountCanListPasskeys() throws Exception {
    insertUserEntity(userEntityId, "test_user_two");
    insertCredential(credentialId, userEntityId, "MacBook passkey");

    mockMvc
        .perform(get("/api/account/passkeys").with(testUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].credentialId").value(credentialId))
        .andExpect(jsonPath("$[0].label").value("MacBook passkey"))
        .andExpect(jsonPath("$[0].createdAt").exists())
        .andExpect(jsonPath("$[0].lastUsedAt").exists());
  }

  @Test
  void deletingPasskeyRequiresOwnershipAndAuditsDeletion() throws Exception {
    insertUserEntity(userEntityId, "test_user_two");
    insertCredential(credentialId, userEntityId, "MacBook passkey");
    insertUserEntity(base64Url(4), "other_user");
    insertCredential(otherUserCredentialId, base64Url(4), "Other passkey");

    mockMvc
        .perform(
            delete("/api/account/passkeys/{credentialId}", otherUserCredentialId)
                .with(csrf())
                .with(testUser()))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            delete("/api/account/passkeys/{credentialId}", credentialId)
                .with(csrf())
                .with(testUser()))
        .andExpect(status().isNoContent());

    Integer credentialRows =
        jdbcTemplate.queryForObject(
            "select count(*) from user_credentials where credential_id = ?",
            Integer.class,
            credentialId);
    assertThat(credentialRows).isZero();

    assertThat(auditEventTypesFor(2L)).containsExactly("PASSKEY_DELETED");
  }

  private void insertUserEntity(String id, String username) {
    jdbcTemplate.update(
        "insert into user_entities(id, name, display_name) values (?, ?, ?)",
        id,
        username,
        username);
  }

  private void insertCredential(String credentialId, String userEntityId, String label) {
    jdbcTemplate.update(
        """
        insert into user_credentials(
          credential_id,
          user_entity_user_id,
          public_key,
          signature_count,
          uv_initialized,
          backup_eligible,
          authenticator_transports,
          public_key_credential_type,
          backup_state,
          attestation_object,
          attestation_client_data_json,
          created,
          last_used,
          label)
        values (?, ?, ?, 0, true, false, 'internal', 'public-key', false, ?, ?, ?, ?, ?)
        """,
        credentialId,
        userEntityId,
        new byte[] {1, 2, 3},
        new byte[] {4, 5, 6},
        new byte[] {7, 8, 9},
        Timestamp.from(Instant.parse("2026-06-10T10:00:00Z")),
        Timestamp.from(Instant.parse("2026-06-10T11:00:00Z")),
        label);
  }

  private java.util.List<String> auditEventTypesFor(Long accountId) {
    return jdbcTemplate.queryForList(
        "select event_type from security_audit_event where account_id = ? order by id",
        String.class,
        accountId);
  }

  private static String base64Url(int seed) {
    byte[] bytes = new byte[16];
    for (int index = 0; index < bytes.length; index++) {
      bytes[index] = (byte) (seed + index);
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
