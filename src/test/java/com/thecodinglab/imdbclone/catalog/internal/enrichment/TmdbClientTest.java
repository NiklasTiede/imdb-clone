package com.thecodinglab.imdbclone.catalog.internal.enrichment;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.thecodinglab.imdbclone.catalog.api.MovieEnrichment.Outcome;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TmdbClientTest {
  private final RestClient.Builder builder = RestClient.builder();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
  private final TmdbClient client = new TmdbClient(builder, new TmdbProperties("synthetic-token"));
  private static final String URL =
      "https://api.themoviedb.org/3/movie/13?language=en-US&append_to_response=credits";

  @Test
  void sendsOnlyProviderCredentialsAndProjectsBoundedFacts() {
    server
        .expect(requestTo(URL))
        .andExpect(header("Authorization", "Bearer synthetic-token"))
        .andExpect(headerDoesNotExist("Cookie"))
        .andRespond(
            withSuccess(
                """
          {"id":13,"imdb_id":"tt0109830","budget":55000000,"revenue":677387716,
           "tagline":"Life is like a box of chocolates.","release_date":"1994-06-23",
           "credits":{"cast":[{"name":"Tom Hanks","character":"Forrest Gump"}],
             "crew":[{"name":"Robert Zemeckis","job":"Director"},
                     {"name":"Eric Roth","job":"Screenplay"},
                     {"name":"Eric Roth","job":"Writer"},
                     {"name":"Someone","job":"Other"}]},
           "production_countries":[{"name":"United States of America"}],
           "production_companies":[{"name":"Paramount"}],
           "spoken_languages":[{"english_name":"English"}],
           "homepage":"https://untrusted.example","title":"Never use this as catalog identity"}
          """,
                MediaType.APPLICATION_JSON));
    var result = client.fetch(13, "tt0109830");
    assertThat(result.outcome()).isEqualTo(Outcome.AVAILABLE);
    assertThat(result.facts().directors()).containsExactly("Robert Zemeckis");
    assertThat(result.facts().writers()).containsExactly("Eric Roth");
    assertThat(result.facts().cast().getFirst().character()).isEqualTo("Forrest Gump");
    assertThat(result.facts().budgetUsd()).isEqualTo(55000000L);
    assertThat(result.facts().revenueUsd()).isEqualTo(677387716L);
    assertThat(result.facts().spokenLanguages()).containsExactly("English");
    assertThat(result.facts().productionCountries()).containsExactly("United States of America");
    assertThat(result.facts().productionCompanies()).containsExactly("Paramount");
    server.verify();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"id\":14,\"imdb_id\":\"tt0109830\"}",
        "{\"id\":13,\"imdb_id\":\"tt9999999\"}",
        "{\"id\":13}",
        "{\"id\":\"13\",\"imdb_id\":\"tt0109830\"}"
      })
  void rejectsMismatchedOrMissingProviderIdentity(String body) {
    server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    assertThat(client.fetch(13, "tt0109830").outcome()).isEqualTo(Outcome.IDENTITY_MISMATCH);
  }

  @Test
  void unknownMoneyIsNotZeroAndOversizedTextAndListsAreBounded() {
    String member = "{\"name\":\"" + "x".repeat(250) + "\",\"character\":null}";
    String body =
        "{\"id\":13,\"budget\":0,\"revenue\":-1,\"credits\":{\"cast\":["
            + String.join(",", java.util.Collections.nCopies(20, member))
            + "]}}";
    server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    var facts = client.fetch(13, null).facts();
    assertThat(facts.budgetUsd()).isNull();
    assertThat(facts.revenueUsd()).isNull();
    assertThat(facts.tagline()).isNull();
    assertThat(facts.directors()).isEmpty();
    assertThat(facts.cast()).hasSize(8);
    assertThat(facts.cast().getFirst().name()).hasSize(200);
  }

  @ParameterizedTest
  @ValueSource(strings = {"not-json", "null", "[]", "{}"})
  void malformedResponsesCannotEscapeAsProviderErrors(String body) {
    server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    assertThat(client.fetch(13, null).outcome())
        .isIn(Outcome.UNAVAILABLE, Outcome.IDENTITY_MISMATCH);
  }

  @Test
  void tooLargeResponsesAndIoFailuresAreUnavailable() {
    server
        .expect(requestTo(URL))
        .andRespond(withSuccess("x".repeat(512 * 1024 + 1), MediaType.APPLICATION_JSON));
    server
        .expect(requestTo(URL))
        .andRespond(withException(new IOException("private provider detail")));
    assertThat(client.fetch(13, null).outcome()).isEqualTo(Outcome.UNAVAILABLE);
    assertThat(client.fetch(13, null).outcome()).isEqualTo(Outcome.UNAVAILABLE);
  }

  @ParameterizedTest
  @ValueSource(ints = {401, 403, 404, 500, 302})
  void failsWithoutForwardingProviderBodies(int status) {
    server
        .expect(requestTo(URL))
        .andRespond(withStatus(HttpStatus.valueOf(status)).body("private provider detail"));
    var result = client.fetch(13, null);
    assertThat(result.outcome()).isEqualTo(status == 404 ? Outcome.NOT_FOUND : Outcome.UNAVAILABLE);
    assertThat(result.facts()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"120", "nonsense", "9999999"})
  void rateLimitBlocksSubsequentCallsWithoutRetrying(String retryAfter) {
    server
        .expect(requestTo(URL))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", retryAfter));
    assertThat(client.fetch(13, null).outcome()).isEqualTo(Outcome.RATE_LIMITED);
    assertThat(client.fetch(99, null).outcome()).isEqualTo(Outcome.RATE_LIMITED);
    server.verify();
  }

  @Test
  void absentCredentialsDisableNetworkAndConfigurationDoesNotPrintSecrets() {
    var properties = new TmdbProperties(null);
    assertThat(new TmdbClient(builder, properties).fetch(13, null).outcome())
        .isEqualTo(Outcome.DISABLED);
    assertThat(new TmdbProperties("synthetic-token").toString()).doesNotContain("synthetic-token");
    server.verify();
  }

  @Test
  void watchProvidersUseTheSameAuthenticatedBoundedClient() {
    server
        .expect(requestTo("https://api.themoviedb.org/3/movie/13/watch/providers"))
        .andExpect(header("Authorization", "Bearer synthetic-token"))
        .andExpect(headerDoesNotExist("Cookie"))
        .andRespond(withSuccess("{\"id\":13,\"results\":{}}", MediaType.APPLICATION_JSON));
    assertThat(client.watchProviders(13).outcome()).isEqualTo(Outcome.AVAILABLE);
    server.verify();
  }

  @Test
  void watchProviderIdentityMustMatchAndRateLimitIsSharedWithDetails() {
    server
        .expect(requestTo("https://api.themoviedb.org/3/movie/13/watch/providers"))
        .andRespond(withSuccess("{\"id\":14,\"results\":{}}", MediaType.APPLICATION_JSON));
    server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
    assertThat(client.watchProviders(13).outcome()).isEqualTo(Outcome.IDENTITY_MISMATCH);
    assertThat(client.fetch(13, null).outcome()).isEqualTo(Outcome.RATE_LIMITED);
    assertThat(client.watchProviders(13).outcome()).isEqualTo(Outcome.RATE_LIMITED);
    server.verify();
  }
}
