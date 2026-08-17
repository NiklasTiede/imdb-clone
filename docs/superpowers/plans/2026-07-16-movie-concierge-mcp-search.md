# Movie Concierge MCP Search Implementation Plan

**Status:** Completed

**Milestone:** M2 — Java read-only MCP Seam

**Product vision:** [Movie Concierge](../../movie-concierge.md)

**Architecture:** [ADR 0001](../../adr/0001-movie-concierge-architecture.md)

## Goal

Expose one protected, stateless `search_movies` MCP tool from the Spring Boot backend. The tool must
reuse Java-owned hybrid retrieval and return a compact grounded result without allowing the Python
Agent to access PostgreSQL or OpenSearch directly.

This slice proves the transport, security, module, schema, and error contracts needed by the
headless Pydantic AI Agent in M3.

## Decisions

- Use `spring-ai-starter-mcp-server-webmvc` with Spring AI 2.0.0.
- Use stateless Streamable HTTP at `/mcp` and the synchronous server model.
- Enable only the MCP tool capability; resources, prompts, and completions remain disabled.
- Disable the global MCP annotation scanner and explicitly register the one stateless tool
  specification. This avoids early BeanPostProcessor creation and empty non-tool providers while
  preserving `@McpTool` metadata and generated schemas.
- Add one Java `assistant` Module that owns the MCP protocol Adapter and workload access control.
- Deepen Catalog behind a public `MovieSearch` Interface shared by REST and MCP Adapters.
- Delete the mixed internal `MovieSearchService` Interface instead of exposing it across Modules.
- Accept a bounded query and optional catalog filters; always search page zero with at most ten
  results to bound model context and inference cost.
- Return a compact versioned projection rather than the full REST `MovieRecord`.
- Authenticate `/mcp` with one workload bearer token. This authenticates the Python workload, not an
  end user, and cannot authorize account mutations.
- Return client-safe validation and generic downstream failure messages without query text,
  OpenSearch details, stack traces, or bearer tokens.

## Task 1: Add The Spring AI MCP Runtime

**Files:**

- Modify: `build.gradle`
- Modify: `gradle.lockfile`
- Modify: `src/main/resources/config/application.properties`
- Modify: `src/main/resources/config/application-dev.properties`
- Modify: `src/main/resources/config/application-prod.properties`

Add the WebMVC MCP server starter through the existing Spring AI BOM. Configure:

```properties
spring.ai.mcp.server.protocol=STATELESS
spring.ai.mcp.server.type=SYNC
spring.ai.mcp.server.name=imdb-clone-domain-tools
spring.ai.mcp.server.capabilities.tool=true
spring.ai.mcp.server.capabilities.resource=false
spring.ai.mcp.server.capabilities.prompt=false
spring.ai.mcp.server.capabilities.completion=false
spring.ai.mcp.server.stateless.mcp-endpoint=/mcp
spring.ai.mcp.server.stateless.disallow-delete=true
```

Use a fixed local-only token in the development profile. Keep the server disabled by default in the
production profile until the Kubernetes secret and network policy milestone; enabling it requires a
non-empty config-tree/Kubernetes secret or startup fails. Never put the token in logs, metrics, test
snapshots, or documentation examples intended for production.

## Task 2: Deepen The Catalog Search Module

**Files:**

- Create: `src/main/java/com/thecodinglab/imdbclone/catalog/api/MovieSearch.java`
- Modify: Catalog search request/result types to expose the `assistant` named Interface
- Modify: `src/main/java/com/thecodinglab/imdbclone/catalog/web/SearchController.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/OpenSearchMovieSearchService.java`
- Delete: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/MovieSearchService.java`

`MovieSearch` exposes only:

```java
PagedResponse<MovieRecord> searchMovies(
    String query, MovieSearchRequest request, int page, int size);
```

The existing REST Adapter and new MCP Adapter call this same Interface. OpenSearch indexing,
documents, query builders, embeddings, and semantic diagnostics remain internal to Catalog.

## Task 3: Add The `search_movies` Tool

**Files:**

- Create: `src/main/java/com/thecodinglab/imdbclone/assistant/package-info.java`
- Create MCP tool, result projection, metrics, and safe exception classes under
  `assistant/internal/mcp`
- Modify: `src/test/java/com/thecodinglab/imdbclone/ModulithArchitectureTest.java`

Inputs:

- required `query` string, maximum 200 characters; an empty string requests filter-only browsing;
- optional start-year range;
- optional runtime range;
- optional genres and movie type;
- optional `limit`, default five and maximum ten.

Output:

- projection schema version;
- catalog movie ID;
- primary and original title;
- movie type, year, runtime, genres;
- IMDb rating/count;
- bounded description and poster token;
- total matches and whether more results are available.

Mark the tool read-only, non-destructive, idempotent, and closed-world. Generate its output schema.
Do not independently rerank, enrich, or invent candidates in the Assistant Module.

## Task 4: Protect `/mcp`

**Files:**

- Create workload security configuration and filter under `assistant/internal/security`
- Modify the existing application security-chain ordering only as needed

Use a dedicated higher-priority stateless Security filter chain matching only `/mcp` and `/mcp/**`:

- disable CSRF, request cache, login, logout, and HTTP Basic for this chain;
- never create or read an HTTP session;
- compare bearer values in constant time;
- grant only `ROLE_MCP_CLIENT` after successful workload authentication;
- return a generic HTTP 401 problem response for missing or invalid credentials;
- never place the token in the Spring Security principal or credentials.

The existing browser/session chain remains unchanged for REST routes.

## Task 5: Verify Tool, Security, And Protocol Contracts

**Files:**

- Add focused unit tests under `src/test/java/com/thecodinglab/imdbclone/assistant`
- Add MCP JSON-RPC integration coverage using the real Spring WebMVC transport
- Modify architecture tests for the new Module and named Interface

Required coverage:

- exact mapping from tool parameters to `MovieSearchRequest`;
- default and maximum result limits;
- compact result mapping and description bounding;
- invalid arguments return a specific but client-safe validation failure;
- downstream Catalog failures return a generic safe failure;
- no raw query or backend exception message appears in logs or MCP responses;
- unauthenticated and invalid-token `/mcp` requests return 401;
- authenticated `initialize`, `tools/list`, and `tools/call` requests succeed;
- `tools/list` exposes only `search_movies` with read-only/closed-world hints;
- the global annotation scanner and empty resource/prompt/completion providers are absent;
- `tools/call` returns the mocked Catalog IDs and never bypasses `MovieSearch`;
- Spring Modulith verification passes.

## Verification

Run narrow tests first, then:

```bash
./gradlew spotlessApply
./gradlew test --tests MovieSearchMcpToolTest --tests '*Mcp*Test'
./gradlew test --tests ModulithArchitectureTest
./gradlew test
./gradlew integrationTest
./gradlew build jacocoTestReport
make verify-agent
git diff --check
```

`make verify-agent` remains unchanged but proves the current Python foundation still accepts the
planned MCP tool name and eval vocabulary. No frontend or Kubernetes check is required because this
slice changes neither browser nor deployment manifests.

### Verification result — 2026-07-16

- focused MCP tool, security, protocol, and Modulith tests passed;
- complete `./gradlew test` passed;
- container-backed `./gradlew integrationTest` passed;
- `./gradlew build jacocoTestReport` passed, including formatting and coverage enforcement;
- `make verify-agent` passed on Python 3.14.5 with Ruff, strict Pyright, Import Linter, and 17 tests;
- `git diff --check` passed.

No commit or deployment was created. Production MCP remains disabled until the encrypted workload
secret and network policy are explicitly added.
