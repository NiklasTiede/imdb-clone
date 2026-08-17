package com.thecodinglab.imdbclone.assistant.internal.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecodinglab.imdbclone.assistant.internal.security.McpSecurityProperties;
import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieSearch;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    classes = McpProtocolContractTest.TestApplication.class,
    properties = {
      "imdb-clone.assistant.mcp.bearer-token=contract-test-token",
      "spring.ai.mcp.server.protocol=STATELESS",
      "spring.ai.mcp.server.type=SYNC",
      "spring.ai.mcp.server.name=imdb-clone-domain-tools",
      "spring.ai.mcp.server.version=1.0.0",
      "spring.ai.mcp.server.capabilities.tool=true",
      "spring.ai.mcp.server.capabilities.resource=false",
      "spring.ai.mcp.server.capabilities.prompt=false",
      "spring.ai.mcp.server.capabilities.completion=false",
      "spring.ai.mcp.server.annotation-scanner.enabled=false",
      "spring.ai.mcp.server.stateless.mcp-endpoint=/mcp",
      "management.endpoint.health.validate-group-membership=false"
    })
@AutoConfigureMockMvc
class McpProtocolContractTest {

  private static final String AUTHORIZATION = "Bearer contract-test-token";

  @Autowired private MockMvc mockMvc;
  @Autowired private CapturingMovieSearch movieSearch;
  @Autowired private ApplicationContext applicationContext;

  @BeforeEach
  void resetMovieSearch() {
    movieSearch.reset();
  }

  @Test
  void initialize_requiresWorkloadAuthentication() throws Exception {
    mockMvc
        .perform(mcpRequest(initializeRequest()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("A valid MCP workload bearer token is required."));
  }

  @Test
  void initialize_negotiatesTheStatelessMcpServer() throws Exception {
    mockMvc
        .perform(authenticatedMcpRequest(initializeRequest()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.jsonrpc").value("2.0"))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.result.protocolVersion").value("2025-11-25"))
        .andExpect(jsonPath("$.result.serverInfo.name").value("imdb-clone-domain-tools"))
        .andExpect(jsonPath("$.result.capabilities.tools").exists());
  }

  @Test
  void explicitToolRegistrationReplacesTheGlobalAnnotationScannerAndEmptyProviders() {
    assertThat(applicationContext.containsBean("movieConciergeToolSpecifications")).isTrue();
    assertThat(applicationContext.containsBean("serverAnnotatedMethodBeanPostProcessor")).isFalse();
    assertThat(applicationContext.containsBean("serverAnnotatedBeanRegistry")).isFalse();
    assertThat(applicationContext.containsBean("resourceSpecs")).isFalse();
    assertThat(applicationContext.containsBean("resourceTemplateSpecs")).isFalse();
    assertThat(applicationContext.containsBean("promptSpecs")).isFalse();
    assertThat(applicationContext.containsBean("completionSpecs")).isFalse();
  }

  @Test
  void toolsListPublishesOnlyTheBoundedReadOnlyMovieSearchContract() throws Exception {
    mockMvc
        .perform(authenticatedMcpRequest(toolsListRequest()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.tools.length()").value(1))
        .andExpect(jsonPath("$.result.tools[0].name").value("search_movies"))
        .andExpect(jsonPath("$.result.tools[0].annotations.readOnlyHint").value(true))
        .andExpect(jsonPath("$.result.tools[0].annotations.destructiveHint").value(false))
        .andExpect(jsonPath("$.result.tools[0].annotations.openWorldHint").value(false))
        .andExpect(jsonPath("$.result.tools[0].inputSchema.properties.query.type").value("string"))
        .andExpect(jsonPath("$.result.tools[0].inputSchema.properties.limit.type").value("integer"))
        .andExpect(jsonPath("$.result.tools[0].outputSchema.properties.movies").exists());
  }

  @Test
  void toolsCallReturnsStructuredGroundedMoviesAndInvokesThePublicCatalogUseCase()
      throws Exception {
    String response =
        mockMvc
            .perform(authenticatedMcpRequest(toolsCallRequest()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.isError").value(false))
            .andExpect(jsonPath("$.result.structuredContent.schemaVersion").value("1.0"))
            .andExpect(jsonPath("$.result.structuredContent.totalMatches").value(4))
            .andExpect(jsonPath("$.result.structuredContent.moreAvailable").value(true))
            .andExpect(
                jsonPath("$.result.structuredContent.movies[0].primaryTitle").value("Arrival"))
            .andExpect(jsonPath("$.result.structuredContent.movies[0].movieId").value(42))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).doesNotContain("backdrop-token", "trailer-key");
    assertThat(movieSearch.invocation())
        .isEqualTo(
            new SearchInvocation(
                "thoughtful science fiction",
                new MovieSearchRequest(
                    1990, null, null, 180, Set.of(MovieGenre.SCI_FI), MovieType.MOVIE),
                0,
                3));
  }

  @Test
  void toolsCallDoesNotExposeCatalogFailureDetails() throws Exception {
    movieSearch.failWith(new IllegalStateException("internal OpenSearch topology"));

    String response =
        mockMvc
            .perform(authenticatedMcpRequest(toolsCallRequest()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.isError").value(true))
            .andExpect(
                jsonPath("$.result.content[0].text")
                    .value(
                        org.hamcrest.Matchers.containsString(
                            "Movie search is temporarily unavailable.")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).doesNotContain("internal OpenSearch topology");
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
      mcpRequest(String content) {
    return post("/mcp")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
        .content(content);
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
      authenticatedMcpRequest(String content) {
    return mcpRequest(content).header(HttpHeaders.AUTHORIZATION, AUTHORIZATION);
  }

  private static String initializeRequest() {
    return """
        {
          "jsonrpc": "2.0",
          "id": 1,
          "method": "initialize",
          "params": {
            "protocolVersion": "2025-11-25",
            "capabilities": {},
            "clientInfo": {"name": "contract-test", "version": "1.0.0"}
          }
        }
        """;
  }

  private static String toolsListRequest() {
    return """
        {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
        """;
  }

  private static String toolsCallRequest() {
    return """
        {
          "jsonrpc": "2.0",
          "id": 3,
          "method": "tools/call",
          "params": {
            "name": "search_movies",
            "arguments": {
              "query": "thoughtful science fiction",
              "minStartYear": 1990,
              "maxRuntimeMinutes": 180,
              "genres": ["SCI_FI"],
              "movieType": "MOVIE",
              "limit": 3
            }
          }
        }
        """;
  }

  @SpringBootConfiguration(proxyBeanMethods = false)
  @EnableAutoConfiguration(
      exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
      })
  @EnableConfigurationProperties(McpSecurityProperties.class)
  @ComponentScan(basePackageClasses = {MovieSearchMcpTool.class, McpSecurityProperties.class})
  static class TestApplication {

    @Bean
    CapturingMovieSearch movieSearch() {
      return new CapturingMovieSearch();
    }

    @Bean
    MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }
  }

  static final class CapturingMovieSearch implements MovieSearch {

    private final AtomicReference<SearchInvocation> invocation = new AtomicReference<>();
    private final AtomicReference<RuntimeException> failure = new AtomicReference<>();

    @Override
    public PagedResponse<MovieRecord> searchMovies(
        String query, MovieSearchRequest request, int page, int size) {
      RuntimeException configuredFailure = failure.get();
      if (configuredFailure != null) {
        throw configuredFailure;
      }
      invocation.set(new SearchInvocation(query, request, page, size));
      return new PagedResponse<>(List.of(movie()), 0, size, 4, 2, false);
    }

    SearchInvocation invocation() {
      return invocation.get();
    }

    void reset() {
      invocation.set(null);
      failure.set(null);
    }

    void failWith(RuntimeException exception) {
      failure.set(exception);
    }

    private static MovieRecord movie() {
      return new MovieRecord(
          42L,
          "tt2543164",
          329865L,
          MovieType.MOVIE,
          "Arrival",
          "Arrival",
          false,
          2016,
          null,
          116,
          null,
          null,
          Set.of(MovieGenre.DRAMA, MovieGenre.SCI_FI),
          7.9F,
          800_000,
          "A linguist works with the military to communicate with alien lifeforms.",
          "poster-token",
          "backdrop-token",
          "trailer-key",
          4.5F,
          100);
    }
  }

  private record SearchInvocation(String query, MovieSearchRequest request, int page, int size) {}
}
