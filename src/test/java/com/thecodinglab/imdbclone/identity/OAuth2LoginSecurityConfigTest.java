package com.thecodinglab.imdbclone.identity;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecodinglab.imdbclone.support.BaseContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.security.oauth2.client.registration.google.client-id=test-google-client",
      "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
      "spring.security.oauth2.client.registration.google.scope=openid,profile,email"
    })
@AutoConfigureMockMvc
class OAuth2LoginSecurityConfigTest extends BaseContainers {

  @Autowired private MockMvc mockMvc;

  @Test
  void oauth2AuthorizationEndpointRedirectsToProvider() throws Exception {
    mockMvc
        .perform(get("/oauth2/authorization/google"))
        .andExpect(status().is3xxRedirection())
        .andExpect(header().string("Location", containsString("accounts.google.com")));
  }
}
