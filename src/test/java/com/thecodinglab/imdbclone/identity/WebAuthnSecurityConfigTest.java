package com.thecodinglab.imdbclone.identity;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecodinglab.imdbclone.support.BaseContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "imdb-clone.identity.webauthn.rp-id=localhost",
      "imdb-clone.identity.webauthn.allowed-origins=http://localhost:3000"
    })
@AutoConfigureMockMvc
class WebAuthnSecurityConfigTest extends BaseContainers {

  @Autowired private MockMvc mockMvc;

  @Test
  void passkeyAuthenticationOptionsAreAvailableBeforeLogin() throws Exception {
    mockMvc
        .perform(post("/webauthn/authenticate/options").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  void passkeyRegistrationOptionsRequireLoggedInAccount() throws Exception {
    mockMvc
        .perform(post("/webauthn/register/options").with(csrf()))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(post("/webauthn/register/options").with(csrf()).with(user("user")))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }
}
