# Kubernetes Readiness Review

## Scope

Review whether the application can run safely as stateless backend containers with multiple
replicas in Kubernetes. Focus on runtime state, repeated startup, graceful shutdown, probes,
background jobs, external dependencies, and configuration.

Primary files:

- `Dockerfile`
- `compose.yaml`
- `Makefile`
- `src/main/resources/config/*.properties`
- `src/main/java/com/thecodinglab/imdbclone/Application.java`
- `src/main/java/com/thecodinglab/imdbclone/*/internal`
- `src/main/java/com/thecodinglab/imdbclone/*/api`
- `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search`
- `src/main/java/com/thecodinglab/imdbclone/identity/internal`
- `src/main/java/com/thecodinglab/imdbclone/media/internal`
- `src/main/resources/db/migration`
- `src/main/resources/sql`
- `docs`
- infrastructure or deployment folders, if present

## Checks

### Stateless Containers

- backend instances do not rely on durable local filesystem writes
- uploaded images and generated media are stored in RustFS or another external object store
- database, Elasticsearch, RustFS, mail, and external APIs are treated as external services
- no feature requires sticky sessions or pod-local auth/session state
- startup can run repeatedly without mutating shared state outside controlled migrations or seed jobs

### Multi-Replica Safety

- scheduled/background work is cluster-safe, idempotent, or protected by a distributed lock/queue
- task execution is at-least-once safe: retries and duplicate execution do not corrupt source-of-truth data
- cleanup jobs, token expiration, projection repair, import jobs, and email jobs behave correctly with replica count greater than one
- unique constraints protect externally visible idempotency where duplicate requests/jobs are plausible
- concurrent writes use database constraints, optimistic locking, or atomic update queries where needed

### Migrations and Startup

- Flyway is the only schema owner
- migrations are repeatable in a clean environment and deterministic across replicas
- application startup does not require manual ordering beyond dependency readiness
- non-schema imports or seed data are not hidden in migrations unless intentionally documented
- multiple pods starting together cannot race on initialization work outside Flyway's own locking

### Probes and Shutdown

- readiness probes reflect critical dependencies needed to serve traffic
- liveness probes do not fail on transient downstream outages that readiness should handle
- actuator health groups are configured intentionally for Kubernetes probes
- shutdown gives HTTP requests and background jobs time to stop or release locks
- workers can recover from pods killed during task execution

### Configuration and Secrets

- environment-specific values can be supplied through environment variables, Kubernetes Secrets, or ConfigMaps
- secrets are not baked into images, source files, frontend bundles, or default config
- public frontend config is separated from backend secrets
- resource limits, JVM memory behavior, ports, and management endpoints are deployable without source changes

### Storage, Search, and Consistency

- PostgreSQL remains the transactional source of truth
- Elasticsearch is treated as rebuildable projection state
- RustFS object lifecycle is tied to DB tokens or durable owner events
- cross-system updates have repair paths when downstream services are unavailable
- queue/task tables are observable enough to diagnose stuck or failing work

### Container Build and Runtime

- image build does not depend on local developer state
- runtime image does not need build tools or writable source folders
- container runs with predictable ports and profile/config selection
- logs go to stdout/stderr and contain enough structured context for aggregation
- no development-only services or credentials are required in production profile

## Evidence Patterns

Look for:

- local file writes: `Files.write`, `new File`, `Path.of`, `MultipartFile.transferTo`
- schedulers and async work: `@Scheduled`, `@Async`, db-scheduler tasks, cleanup services
- startup hooks: `CommandLineRunner`, `ApplicationRunner`, `@PostConstruct`, Flyway callbacks
- profiles/config: `application*.properties`, `@Profile`, `${...}` placeholders
- probes/management: `management.endpoint.health`, health groups, actuator exposure
- external clients: `DataSource`, Elasticsearch repositories/clients, RustFS, mail, REST clients

## Report Guidance

Prefer concrete Kubernetes failure scenarios:

- "With two replicas, both pods can process the same cleanup job."
- "A pod restart loses state because the token is only held in memory."
- "Readiness can go green before required indexes/tables exist."
- "A failed Elasticsearch write is repairable because a durable task remains."

Do not require Helm or Kubernetes manifests unless the user asks for deployment implementation.
Use `gitops` mode for manifest ownership and `observability` mode for metrics, logs,
Prometheus, and Grafana contracts. Report missing manifests as a readiness gap only when
the request is explicitly about deployment assets.
