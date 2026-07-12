package com.thecodinglab.imdbclone.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.thecodinglab.imdbclone.support.BaseControllerIntegrationTest;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

class OpenApiContractIntegrationTest extends BaseControllerIntegrationTest {

  private static final Path FRONTEND_OPENAPI_SPEC =
      Path.of("frontend", "src", "client", "imdb-clone-backend.yaml");
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
  private static final ObjectMapper YAML_MAPPER = new YAMLMapper();

  @Autowired private MockMvc mockMvc;

  @Test
  @WithMockUser
  void checkedInFrontendContractMatchesBackend() throws Exception {
    String generatedContract =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode expected =
        withoutEnvironmentSpecificServers(YAML_MAPPER.readTree(FRONTEND_OPENAPI_SPEC.toFile()));
    JsonNode actual = withoutEnvironmentSpecificServers(JSON_MAPPER.readTree(generatedContract));

    assertThat(actual)
        .as("Backend OpenAPI contract drifted; refresh the frontend spec and regenerate its client")
        .isEqualTo(expected);
  }

  private static JsonNode withoutEnvironmentSpecificServers(JsonNode contract) {
    if (contract instanceof ObjectNode objectContract) {
      objectContract.remove("servers");
    }
    return contract;
  }
}
