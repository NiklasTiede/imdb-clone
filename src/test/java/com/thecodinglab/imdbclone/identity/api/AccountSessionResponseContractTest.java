package com.thecodinglab.imdbclone.identity.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

class AccountSessionResponseContractTest {

  @Test
  void exposesEverySessionFieldAsRequiredInOpenApi() {
    var schemas = ModelConverters.getInstance(true).read(AccountSessionResponse.class);
    Schema<?> schema = schemas.get(AccountSessionResponse.class.getSimpleName());

    assertThat(schema).isNotNull();
    assertThat(schema.getRequired()).containsExactlyInAnyOrder("id", "username", "email", "roles");
  }
}
