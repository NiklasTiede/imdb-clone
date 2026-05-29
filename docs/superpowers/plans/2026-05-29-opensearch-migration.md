# OpenSearch Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Elasticsearch with OpenSearch in local development, backend dependencies, tests, and k3s GitOps, while preserving existing movie search behavior first.

**Architecture:** Keep the backend search module boundary stable: `MovieSearchService` remains the application interface and `MovieSearchDocument` remains the search projection. Migrate the concrete adapter, repository, configuration, and infrastructure from Elasticsearch to OpenSearch, then add native OpenSearch RRF as a separate follow-up so parity failures are easier to diagnose.

**Tech Stack:** Spring Boot 4, Java 25, OpenSearch 3.x, OpenSearch Java client, Spring Data OpenSearch, Testcontainers, Docker Compose, k3s Kustomize/GitOps.

---

## Migration Principles

- Do not change API responses, frontend behavior, or search relevance in the first migration pass.
- Keep Java-side RRF until OpenSearch is running locally, in tests, and in k3s.
- Rename public operational vocabulary from Elasticsearch to OpenSearch where it affects configuration, manifests, docs, and logs.
- Preserve backward-compatible config aliases only if needed for a short transition. Remove them before the final verification if all call sites are migrated.
- Do not decrypt or print `*.sops.yaml` secrets. For secret key renames, edit encrypted files only through the existing SOPS workflow when explicitly approved.

## File Map

- Modify `build.gradle` to replace Spring Data Elasticsearch/Testcontainers Elasticsearch dependencies with OpenSearch equivalents.
- Modify `gradle.properties` to add pinned OpenSearch dependency versions.
- Modify `compose.yaml` to replace the Elasticsearch service and volume with OpenSearch.
- Modify `src/main/resources/config/application*.properties` to use OpenSearch endpoint properties and health naming.
- Modify `src/main/java/com/thecodinglab/imdbclone/Application.java` to use OpenSearch repository configuration.
- Rename `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/ElasticsearchMovieSearchService.java` to `OpenSearchMovieSearchService.java`.
- Rename `src/main/java/com/thecodinglab/imdbclone/shared/error/ElasticsearchOperationException.java` to `OpenSearchOperationException.java`.
- Modify `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/query/MovieSearchQueryBuilder.java` for OpenSearch request/query imports if the Java client package changes require it.
- Modify `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/index/MovieSearchDocumentRepository.java` to extend the OpenSearch repository type.
- Modify `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/index/MovieSearchIndexMaintenance.java` to use OpenSearch operations/template types.
- Modify `src/test/java/com/thecodinglab/imdbclone/support/BaseContainers.java` to use an OpenSearch container.
- Rename/update search tests that refer to Elasticsearch.
- Modify `infrastructure/clusters/home/apps/kustomization.yaml`, replace `infrastructure/clusters/home/apps/elasticsearch.yaml` with `opensearch.yaml`, and update backend service URLs.
- Modify `infrastructure/clusters/home/apps/backend.yaml` and `application-prod.properties` service/secret names.
- Modify docs and `.http` request files after the working code/infrastructure migration.

---

## Task 1: Dependency Baseline

**Files:**
- Modify: `gradle.properties`
- Modify: `build.gradle`

- [ ] **Step 1: Add OpenSearch versions**

Add these properties to `gradle.properties`:

```properties
springBootVersion=4.0.6
springAiVersion=2.0.0-M7
testContainersVersion=2.0.5
jwtVersion=0.13.0
mapstructVersion=1.6.3
springModulithVersion=2.0.6
awsSdkVersion=2.44.4
springDataOpenSearchVersion=3.0.5
opensearchTestcontainersVersion=2.0.1
```

- [ ] **Step 2: Replace search dependencies**

In `build.gradle`, replace:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'
implementation 'org.springframework.ai:spring-ai-model'
```

with:

```gradle
implementation "org.opensearch.client:spring-data-opensearch-starter:${springDataOpenSearchVersion}"
implementation 'org.springframework.ai:spring-ai-model'
```

Replace:

```gradle
testImplementation "org.testcontainers:testcontainers-elasticsearch:${testContainersVersion}"
```

with:

```gradle
testImplementation "org.opensearch:opensearch-testcontainers:${opensearchTestcontainersVersion}"
```

- [ ] **Step 3: Verify dependency resolution**

Run:

```bash
./gradlew dependencies --configuration runtimeClasspath
```

Expected: dependency report completes and includes `org.opensearch.client:spring-data-opensearch-starter`.

- [ ] **Step 4: Commit**

```bash
git add build.gradle gradle.properties
git commit -m "build: add opensearch dependencies"
```

---

## Task 2: Local Docker Compose

**Files:**
- Modify: `compose.yaml`
- Modify: `src/main/resources/config/application-dev.properties`

- [ ] **Step 1: Replace the Compose search service**

In `compose.yaml`, replace the `imdb-clone-elasticsearch` service with:

```yaml
  ### ----------------------- OpenSearch --------------------- ###
  imdb-clone-opensearch:
    container_name: imdb-clone-opensearch
    image: opensearchproject/opensearch:3.6.0
    restart: unless-stopped
    environment:
      - discovery.type=single-node
      - plugins.security.disabled=true
      - OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx2g
      - DISABLE_INSTALL_DEMO_CONFIG=true
    ports:
      - "9200:9200"
    volumes:
      - imdb-clone-opensearch-data:/usr/share/opensearch/data
    networks:
      - imdb-clone-network
```

Replace the volume:

```yaml
  imdb-clone-elasticsearch-data:
    name: imdb-clone-elasticsearch-data
```

with:

```yaml
  imdb-clone-opensearch-data:
    name: imdb-clone-opensearch-data
```

- [ ] **Step 2: Update dev config**

In `src/main/resources/config/application-dev.properties`, replace:

```properties
### ------------ Elasticsearch Config -----------------
spring.elasticsearch.uris=http://localhost:9200
spring.elasticsearch.username=elastic
spring.elasticsearch.password=password
```

with:

```properties
### ------------ OpenSearch Config -----------------
spring.opensearch.uris=http://localhost:9200
spring.opensearch.username=
spring.opensearch.password=
```

- [ ] **Step 3: Start local dependencies**

Run:

```bash
make docker-compose-dev-up
```

Expected: OpenSearch container starts and `curl http://localhost:9200` returns an OpenSearch JSON response.

- [ ] **Step 4: Commit**

```bash
git add compose.yaml src/main/resources/config/application-dev.properties
git commit -m "chore: run opensearch locally"
```

---

## Task 3: Spring Boot OpenSearch Wiring

**Files:**
- Modify: `src/main/java/com/thecodinglab/imdbclone/Application.java`
- Modify: `src/main/resources/config/application.properties`
- Modify: `src/main/resources/config/application-prod.properties`
- Modify: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/index/MovieSearchDocumentRepository.java`

- [ ] **Step 1: Update application repository imports**

In `Application.java`, replace Elasticsearch repository imports with OpenSearch repository imports:

```java
import org.opensearch.data.repository.OpenSearchRepository;
import org.opensearch.data.repository.config.EnableOpenSearchRepositories;
```

Replace:

```java
@EnableElasticsearchRepositories(
    includeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = ElasticsearchRepository.class))
```

with:

```java
@EnableOpenSearchRepositories(
    includeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = OpenSearchRepository.class))
```

- [ ] **Step 2: Update repository interface**

In `MovieSearchDocumentRepository.java`, replace the content with:

```java
package com.thecodinglab.imdbclone.catalog.internal.search.index;

import org.opensearch.data.repository.OpenSearchRepository;

public interface MovieSearchDocumentRepository
    extends OpenSearchRepository<MovieSearchDocument, Long> {}
```

- [ ] **Step 3: Update base health/config naming**

In `src/main/resources/config/application.properties`, replace:

```properties
### ------------ Elasticsearch Config -----------------
management.health.elasticsearch.enabled=true
```

with:

```properties
### ------------ OpenSearch Config -----------------
management.health.opensearch.enabled=true
```

In `src/main/resources/config/application-prod.properties`, replace:

```properties
### ------------ Elasticsearch Config -----------------
spring.elasticsearch.uris=http://imdb-clone-elasticsearch:9200
spring.elasticsearch.username=${elasticsearch_username}
spring.elasticsearch.password=${elasticsearch_password}
```

with:

```properties
### ------------ OpenSearch Config -----------------
spring.opensearch.uris=http://imdb-clone-opensearch:9200
spring.opensearch.username=${opensearch_username:}
spring.opensearch.password=${opensearch_password:}
```

- [ ] **Step 4: Compile to expose package mismatches**

Run:

```bash
./gradlew compileJava
```

Expected: compile fails only on remaining Elasticsearch imports in search adapter, query builder, tests, and operations classes.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/thecodinglab/imdbclone/Application.java src/main/resources/config/application.properties src/main/resources/config/application-prod.properties src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/index/MovieSearchDocumentRepository.java
git commit -m "refactor: wire opensearch repositories"
```

---

## Task 4: Backend Search Adapter Rename

**Files:**
- Rename: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/ElasticsearchMovieSearchService.java` to `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/OpenSearchMovieSearchService.java`
- Rename: `src/main/java/com/thecodinglab/imdbclone/shared/error/ElasticsearchOperationException.java` to `src/main/java/com/thecodinglab/imdbclone/shared/error/OpenSearchOperationException.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/query/MovieSearchQueryBuilder.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/MovieSearchService.java`

- [ ] **Step 1: Rename exception**

Rename the file and replace its class body with:

```java
package com.thecodinglab.imdbclone.shared.error;

public class OpenSearchOperationException extends RuntimeException {

  public OpenSearchOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}
```

- [ ] **Step 2: Rename search service**

Rename `ElasticsearchMovieSearchService` to `OpenSearchMovieSearchService`. Replace imports from:

```java
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
```

to the corresponding OpenSearch Java client imports:

```java
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.*;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.search.Hit;
```

Replace the field and constructor parameter:

```java
private final OpenSearchClient openSearchClient;
```

and use `openSearchClient` for `index`, `bulk`, `get`, and `search` calls.

Replace exception throws with `OpenSearchOperationException` and log text with `OpenSearch`.

- [ ] **Step 3: Update query builder imports**

In `MovieSearchQueryBuilder.java`, replace:

```java
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
```

with:

```java
import org.opensearch.client.opensearch._types.query_dsl.*;
import org.opensearch.client.opensearch.core.SearchRequest;
```

- [ ] **Step 4: Update service interface import**

In `MovieSearchService.java`, replace:

```java
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
```

with:

```java
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
```

- [ ] **Step 5: Compile**

Run:

```bash
./gradlew compileJava
```

Expected: main code compiles, or errors identify exact OpenSearch Java client builder differences to fix in this task before continuing.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/thecodinglab/imdbclone/catalog/internal/search src/main/java/com/thecodinglab/imdbclone/shared/error src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/query/MovieSearchQueryBuilder.java src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/MovieSearchService.java
git commit -m "refactor: migrate search adapter to opensearch"
```

---

## Task 5: Index Operations and Mapping Parity

**Files:**
- Modify: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/index/MovieSearchIndexMaintenance.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/index/MovieSearchDocument.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/catalog/internal/search/index/MovieSearchIndexMaintenanceTest.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/catalog/SearchControllerTest.java`

- [ ] **Step 1: Update OpenSearch operations imports**

In `MovieSearchIndexMaintenance.java`, replace Spring Data Elasticsearch operations imports with OpenSearch equivalents:

```java
import org.opensearch.data.core.OpenSearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
```

Keep `IndexOperations` from Spring Data Elasticsearch if Spring Data OpenSearch still exposes that type through its Elasticsearch base. If compilation shows a package move, replace it with the OpenSearch package indicated by the compiler.

- [ ] **Step 2: Preserve index mapping expectations**

Keep `MovieSearchDocument` fields unchanged for the first migration:

```java
@Field(type = FieldType.Search_As_You_Type)
private String primaryTitle;

@Field(type = FieldType.Search_As_You_Type)
private String originalTitle;

@Field(
    type = FieldType.Dense_Vector,
    dims = 768,
    index = true,
    knnSimilarity = KnnSimilarity.COSINE)
private float[] embedding;
```

If OpenSearch rejects `Dense_Vector`, replace only the embedding annotation with the OpenSearch-supported k-NN mapping path in a dedicated follow-up commit; do not change title mappings in the same commit.

- [ ] **Step 3: Update tests to assert OpenSearch mapping names**

In `SearchControllerTest`, keep the mapping assertions:

```java
Map<String, Object> embeddingMapping =
    propertyMapping(openSearchOperations.indexOps(MovieSearchDocument.class).getMapping(), "embedding");
Map<String, Object> primaryTitleMapping =
    propertyMapping(openSearchOperations.indexOps(MovieSearchDocument.class).getMapping(), "primaryTitle");
Map<String, Object> originalTitleMapping =
    propertyMapping(openSearchOperations.indexOps(MovieSearchDocument.class).getMapping(), "originalTitle");
```

Expected values remain:

```java
assertThat(embeddingMapping).containsEntry("dims", 768);
assertThat(primaryTitleMapping).containsEntry("type", "search_as_you_type");
assertThat(originalTitleMapping).containsEntry("type", "search_as_you_type");
```

- [ ] **Step 4: Run focused mapping tests**

Run:

```bash
./gradlew test --tests MovieSearchIndexMaintenanceTest --tests SearchControllerTest
```

Expected: tests pass or fail only because OpenSearch mapping differs; fix mapping before moving on.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/index src/test/java/com/thecodinglab/imdbclone/catalog/internal/search/index/MovieSearchIndexMaintenanceTest.java src/test/java/com/thecodinglab/imdbclone/catalog/SearchControllerTest.java
git commit -m "test: preserve opensearch index mapping"
```

---

## Task 6: Testcontainers Migration

**Files:**
- Modify: `src/test/java/com/thecodinglab/imdbclone/support/BaseContainers.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/catalog/internal/search/ElasticsearchMovieSearchServiceTest.java`
- Rename: `src/test/java/com/thecodinglab/imdbclone/catalog/internal/search/ElasticsearchMovieSearchServiceTest.java` to `src/test/java/com/thecodinglab/imdbclone/catalog/internal/search/OpenSearchMovieSearchServiceTest.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/catalog/internal/search/query/MovieSearchQueryBuilderTest.java`

- [ ] **Step 1: Replace Elasticsearch test container**

In `BaseContainers.java`, replace:

```java
import org.testcontainers.elasticsearch.ElasticsearchContainer;
```

with:

```java
import org.opensearch.testcontainers.OpensearchContainer;
```

Replace image and container fields with:

```java
private static final DockerImageName openSearchImage =
    DockerImageName.parse("opensearchproject/opensearch:3.6.0");

static OpensearchContainer<?> openSearchContainer =
    new OpensearchContainer<>(openSearchImage)
        .withEnv("plugins.security.disabled", "true")
        .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
        .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
        .withStartupTimeout(Duration.ofMinutes(3));
```

Replace dynamic properties with:

```java
@DynamicPropertySource
static void openSearchProperties(DynamicPropertyRegistry registry) {
  registry.add("spring.opensearch.uris", openSearchContainer::getHttpHostAddress);
  registry.add("spring.opensearch.username", () -> "");
  registry.add("spring.opensearch.password", () -> "");
}
```

- [ ] **Step 2: Update unit test imports**

In search unit tests, replace `co.elastic.clients.elasticsearch` imports with `org.opensearch.client.opensearch` imports.

- [ ] **Step 3: Run focused search tests**

Run:

```bash
./gradlew test --tests OpenSearchMovieSearchServiceTest --tests MovieSearchQueryBuilderTest --tests MovieSearchRankFusionTest
```

Expected: all focused search unit tests pass.

- [ ] **Step 4: Run integration tests using OpenSearch**

Run:

```bash
./gradlew test --tests SearchControllerTest --tests MovieControllerTest
```

Expected: OpenSearch Testcontainer starts, index projection works, lexical/hybrid/semantic search tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/thecodinglab/imdbclone/support/BaseContainers.java src/test/java/com/thecodinglab/imdbclone/catalog/internal/search src/test/java/com/thecodinglab/imdbclone/catalog/internal/search/query/MovieSearchQueryBuilderTest.java
git commit -m "test: run search tests on opensearch"
```

---

## Task 7: k3s GitOps Migration

**Files:**
- Delete: `infrastructure/clusters/home/apps/elasticsearch.yaml`
- Create: `infrastructure/clusters/home/apps/opensearch.yaml`
- Modify: `infrastructure/clusters/home/apps/kustomization.yaml`
- Modify: `infrastructure/clusters/home/apps/backend.yaml`
- Modify: `infrastructure/clusters/home/apps/backend-runtime.sops.yaml` through SOPS only, if secrets are still needed.

- [ ] **Step 1: Replace Kustomize resource**

In `infrastructure/clusters/home/apps/kustomization.yaml`, replace:

```yaml
  - elasticsearch.yaml
```

with:

```yaml
  - opensearch.yaml
```

- [ ] **Step 2: Create OpenSearch manifest**

Create `infrastructure/clusters/home/apps/opensearch.yaml`:

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: imdb-clone-opensearch
  namespace: databases
  annotations:
    argocd.argoproj.io/sync-wave: "2"
spec:
  serviceName: imdb-clone-opensearch
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: imdb-clone-opensearch
  template:
    metadata:
      labels:
        app.kubernetes.io/name: imdb-clone-opensearch
    spec:
      containers:
        - name: opensearch
          image: opensearchproject/opensearch:3.6.0
          ports:
            - name: http
              containerPort: 9200
          env:
            - name: discovery.type
              value: single-node
            - name: plugins.security.disabled
              value: "true"
            - name: DISABLE_INSTALL_DEMO_CONFIG
              value: "true"
            - name: OPENSEARCH_JAVA_OPTS
              value: "-Xms512m -Xmx512m"
          resources:
            requests:
              cpu: 500m
              memory: 1Gi
            limits:
              cpu: "1"
              memory: 1Gi
          volumeMounts:
            - name: opensearch-data
              mountPath: /usr/share/opensearch/data
  volumeClaimTemplates:
    - metadata:
        name: opensearch-data
      spec:
        accessModes:
          - ReadWriteOnce
        storageClassName: local-path
        resources:
          requests:
            storage: 10Gi
---
apiVersion: v1
kind: Service
metadata:
  name: imdb-clone-opensearch
  namespace: databases
  annotations:
    argocd.argoproj.io/sync-wave: "3"
spec:
  type: ClusterIP
  selector:
    app.kubernetes.io/name: imdb-clone-opensearch
  ports:
    - name: http
      port: 9200
      targetPort: 9200
```

- [ ] **Step 3: Update backend service URL**

In `infrastructure/clusters/home/apps/backend.yaml`, replace:

```yaml
value: http://imdb-clone-elasticsearch.databases.svc.cluster.local:9200
```

with:

```yaml
value: http://imdb-clone-opensearch.databases.svc.cluster.local:9200
```

If the env var name still says Elasticsearch, rename it to the OpenSearch property name used by Spring Data OpenSearch:

```yaml
- name: SPRING_OPENSEARCH_URIS
  value: http://imdb-clone-opensearch.databases.svc.cluster.local:9200
```

- [ ] **Step 4: Decide security posture before production sync**

For the first home-cluster migration, keep OpenSearch security disabled only if the service remains `ClusterIP`, has no ingress, and only backend pods can reach it by namespace policy. If security must be enabled, stop this task and create a separate secret-management plan for OpenSearch internal users, TLS, and backend credentials.

- [ ] **Step 5: Render manifests**

Run:

```bash
kubectl kustomize infrastructure/clusters/home/apps >/tmp/imdb-clone-home-apps.yaml
```

Expected: rendered YAML contains `StatefulSet/imdb-clone-opensearch`, no `Elasticsearch` custom resource, and backend env points at `imdb-clone-opensearch`.

- [ ] **Step 6: Commit**

```bash
git add infrastructure/clusters/home/apps/kustomization.yaml infrastructure/clusters/home/apps/opensearch.yaml infrastructure/clusters/home/apps/backend.yaml
git rm infrastructure/clusters/home/apps/elasticsearch.yaml
git commit -m "infra: deploy opensearch in k3s"
```

---

## Task 8: Docs and Operational Renames

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/agents/README.md`
- Modify: `docs/agents/verification.md`
- Modify: `docs/development.md`
- Modify: `docs/design.md` only if diagrams/text mention Elasticsearch
- Rename: `src/main/resources/api-calls/elasticsearch/` to `src/main/resources/api-calls/opensearch/`
- Modify: `src/main/resources/api-calls/Actuator.http`
- Modify: `docs/assets/imdb-clone-flow-schema.svg` only if a diagram update workflow exists; otherwise leave it and note it as residual docs debt.

- [ ] **Step 1: Replace user-facing terminology**

Replace prose references from `Elasticsearch` / `ElasticSearch` to `OpenSearch` where they describe the current stack.

Keep historical references only if explicitly describing the migration.

- [ ] **Step 2: Update verification docs**

Ensure `docs/agents/verification.md` includes:

```bash
./gradlew test --tests SearchControllerTest --tests MovieControllerTest
kubectl kustomize infrastructure/clusters/home/apps >/tmp/imdb-clone-home-apps.yaml
```

- [ ] **Step 3: Update HTTP scratch requests**

Rename:

```bash
git mv src/main/resources/api-calls/elasticsearch src/main/resources/api-calls/opensearch
```

In those files, replace titles/comments with `OpenSearch`.

- [ ] **Step 4: Commit**

```bash
git add AGENTS.md docs/agents/README.md docs/agents/verification.md docs/development.md src/main/resources/api-calls src/main/resources/api-calls/Actuator.http
git commit -m "docs: rename search engine to opensearch"
```

---

## Task 9: Full Verification Before Behavior Change

**Files:**
- No code changes unless verification exposes defects.

- [ ] **Step 1: Run backend tests**

Run:

```bash
./gradlew test
```

Expected: all backend tests pass against OpenSearch Testcontainers.

- [ ] **Step 2: Run backend build**

Run:

```bash
./gradlew build jacocoTestReport
```

Expected: build and coverage report complete.

- [ ] **Step 3: Run local smoke**

Run:

```bash
make docker-compose-dev-up
./gradlew bootRun
```

Expected: application starts and actuator readiness is healthy.

In a second terminal, run:

```bash
make seed-light SEED_VERSION=2026-05-17
make reindex-local-search
```

Expected: movies index into OpenSearch and search endpoints return movie results.

- [ ] **Step 4: Render k3s manifests**

Run:

```bash
kubectl kustomize infrastructure/clusters/home/apps >/tmp/imdb-clone-home-apps.yaml
```

Expected: command exits 0.

- [ ] **Step 5: Commit fixes if needed**

If verification required fixes:

```bash
git add <fixed-files>
git commit -m "fix: complete opensearch migration"
```

---

## Task 10: Native OpenSearch RRF Follow-Up

**Files:**
- Modify: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/OpenSearchMovieSearchService.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/query/MovieSearchQueryBuilder.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/catalog/internal/search/OpenSearchMovieSearchServiceTest.java`
- Keep: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/query/MovieSearchRankFusion.java` until native RRF has parity tests.

- [ ] **Step 1: Add a parity test before native RRF**

In `OpenSearchMovieSearchServiceTest`, add a test that documents current hybrid behavior:

```java
@Test
void searchMovies_withTextQueryUsesEqualWeightHybridRanking() throws IOException {
  OpenSearchMovieSearchService service = searchService();
  MovieSearchRequest request = new MovieSearchRequest(null, null, null, null, Set.of(), null);
  when(movieEmbeddingClient.embedText("space horror")).thenReturn(new float[] {0.1f, 0.2f});
  when(openSearchClient.search(searchRequestCaptor.capture(), eq(MovieSearchDocument.class)))
      .thenReturn(
          searchResponse(List.of(movie(2, "Lexical First"), movie(1, "Shared Match"))),
          searchResponse(List.of(movie(1, "Shared Match"), movie(3, "Semantic Only"))));

  PagedResponse<MovieRecord> response = service.searchMovies("space horror", request, 0, 20);

  assertThat(response.getContent()).extracting(MovieRecord::id).containsExactly(1L, 2L, 3L);
}
```

- [ ] **Step 2: Create OpenSearch search pipeline setup**

Add an index/search pipeline initializer only after confirming the OpenSearch Java client supports the required `hybrid` query and `score-ranker-processor` APIs in the pinned version. The search pipeline body must be:

```json
{
  "description": "Post processor for movie hybrid RRF search",
  "phase_results_processors": [
    {
      "score-ranker-processor": {
        "combination": {
          "technique": "rrf",
          "rank_constant": 60,
          "parameters": {
            "weights": [0.5, 0.5]
          }
        }
      }
    }
  ]
}
```

- [ ] **Step 3: Switch hybrid search from two requests to one pipeline-backed request**

Build a single OpenSearch hybrid query with:

```json
{
  "query": {
    "hybrid": {
      "queries": [
        { "...lexical bool/function_score query..." : {} },
        { "...knn query against embedding..." : {} }
      ],
      "filter": [
        "...shared filters..."
      ]
    }
  },
  "search_pipeline": "movie-hybrid-rrf",
  "from": 0,
  "size": 20
}
```

Do not remove `MovieSearchRankFusion` until integration tests prove native RRF returns the expected ordering for overlap, semantic-only, and lexical-only documents.

- [ ] **Step 4: Run relevance regression tests**

Run:

```bash
./gradlew test --tests MovieSearchRankFusionTest --tests OpenSearchMovieSearchServiceTest --tests SearchControllerTest
```

Expected: all tests pass. If ranking differs only in documented tie behavior, update tests to assert the OpenSearch behavior and record the difference in `docs/development.md`.

- [ ] **Step 5: Remove Java RRF only after parity**

Delete `MovieSearchRankFusion.java` and `MovieSearchRankFusionTest.java` only after native RRF passes integration tests.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/thecodinglab/imdbclone/catalog/internal/search src/test/java/com/thecodinglab/imdbclone/catalog/internal/search docs/development.md
git commit -m "feat: use opensearch rrf pipeline"
```

---

## Rollback Plan

- Before k3s sync, rollback is `git revert` of migration commits.
- After k3s sync, rollback requires restoring `elasticsearch.yaml`, backend URL/env names, and the Elasticsearch dependency commits, then re-running search reindex.
- Search data is rebuildable from PostgreSQL through `make reindex-local-search`; do not attempt to migrate index files or PVC contents between Elasticsearch and OpenSearch.
- Keep the old Elasticsearch PVC until OpenSearch search smoke tests pass in k3s, then delete it only with explicit approval.

## Final Verification Matrix

Run these before claiming the migration complete:

```bash
./gradlew test
./gradlew build jacocoTestReport
make docker-compose-dev-up
make seed-light SEED_VERSION=2026-05-17
make reindex-local-search
kubectl kustomize infrastructure/clusters/home/apps >/tmp/imdb-clone-home-apps.yaml
cd frontend && yarn run lint
cd frontend && yarn test
cd frontend && yarn build
```

Expected:

- Backend compiles and tests against OpenSearch.
- Local Compose starts OpenSearch instead of Elasticsearch.
- Movie search index is recreated from PostgreSQL.
- k3s manifests render without the ECK `Elasticsearch` custom resource.
- Frontend remains unchanged behaviorally.

## Self-Review

- Spec coverage: local Docker Compose, dependency exchange, backend code, Testcontainers, k3s deployment, docs, verification, and native RRF follow-up are covered.
- Placeholder scan: no task is left as an unspecified implementation step; where client APIs may differ, the plan gives the exact compile-and-fix checkpoint.
- Type consistency: the plan consistently uses `OpenSearchMovieSearchService`, `OpenSearchOperationException`, `OpenSearchRepository`, and `spring.opensearch.*` naming.
