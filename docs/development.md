# Development Guide

This guide documents the local and validation workflows for the IMDB clone. Run commands from the
repository root unless a command says otherwise.

## Prerequisites

- Java 25
- Docker with Compose
- Node.js 24
- Yarn
- Python 3.14
- uv
- Make
- Optional for deployment validation: `kubectl`, cluster access, and SOPS/age tooling

Check local tools:

```bash
make check-local-tools
make check-agent-tools
```

## Repository Layout

- Backend source: `src/main/java/com/thecodinglab/imdbclone`
- Backend config: `src/main/resources/config`
- Backend migrations: `src/main/resources/db/migration`
- Backend tests: `src/test/java/com/thecodinglab/imdbclone`
- Frontend source: `frontend/src`
- Frontend e2e tests: `frontend/e2e`
- Movie Concierge source: `agent/src/imdb_agent`
- Movie Concierge tests and evals: `agent/tests` and `agent/evals`
- Local stateful services: `compose.yaml`
- k3s GitOps manifests: `infrastructure/clusters/home/apps`
- Seed pipeline: `infrastructure/movie-seed`

## Run Stateful Services

Start PostgreSQL, OpenSearch, and RustFS:

```bash
make docker-compose-dev-up
```

Equivalent direct command:

```bash
docker compose up -d
```

Stop services:

```bash
make docker-compose-dev-down
```

The Compose setup creates these local services:

| Service | URL or port | Notes |
| --- | --- | --- |
| PostgreSQL | `localhost:5432` | Database `movie_db`; local demo credentials only. |
| OpenSearch | `http://localhost:9200` | Security disabled for local development. |
| RustFS API | `http://localhost:9000` | S3-compatible object storage. |
| RustFS console | `http://localhost:9001` | Local console for object storage. |

The `imdb-clone-rustfs-init` container creates the `imdb-clone` bucket and makes
`imdb-clone/movies/*` publicly readable for local media.

## Run Backend

### Configure local social login

Create separate development OAuth applications in Google and GitHub, then copy the ignored local
credentials file:

```bash
cp .env.example .env.local
```

Replace all four values in `.env.local`. The default `dev,local-secrets` Spring profiles import this
file when the backend is started from the repository root. Environment variables with the same names
still take precedence. Tests activate only the `dev` profile and use inert test credentials, so local
secrets never affect automated test results.

Use these provider settings:

| Provider | Setting | Local value |
| --- | --- | --- |
| GitHub | Homepage URL | `http://localhost:3000` |
| GitHub | Authorization callback URL | `http://localhost:3000/login/oauth2/code/github` |
| Google | Application type | Web application |
| Google | Authorized JavaScript origin | `http://localhost:3000` |
| Google | Authorized redirect URI | `http://localhost:3000/login/oauth2/code/google` |

Keep GitHub device flow disabled; this app uses the browser-based authorization-code flow. If the
Google consent screen is in testing mode, add the developers who need local access as test users.
Port `3000` is intentional: OAuth starts on the Vite origin and its `/oauth2` and `/login/oauth2`
routes are proxied to the backend.

Start the backend after local services are running:

```bash
./gradlew bootRun
```

Useful URLs:

- Backend API: `http://localhost:8080`
- Health: `http://localhost:8081/actuator/health`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`
- Swagger UI: `http://localhost:8080/v3/swagger-ui.html`

Flyway runs on startup and applies migrations from `src/main/resources/db/migration`.

## Seed Local Data

After the backend and stateful services are running, seed local users and a lightweight movie/media
catalog:

```bash
make seed-local-users
make seed-light SEED_VERSION=2026-05-17
make reindex-local-search
```

Notes:

- `seed-local-users` creates roles and demo accounts from `src/main/resources/sql/local-users.sql`.
- `seed-light` runs the versioned lightweight seed image against local PostgreSQL and RustFS.
- `reindex-local-search` logs in with a local demo admin account and rebuilds the OpenSearch movie
  index from PostgreSQL.
- The seed pipeline is intended to be idempotent for movie/media upserts and should not wipe local user
  data.

## Run Frontend

Install dependencies and start Vite:

```bash
cd frontend
yarn install
yarn run build:moviesGen
yarn start
```

The frontend runs on `http://localhost:3000` and expects:

- `VITE_IMDB_CLONE_BACKEND_ADDRESS`
- `VITE_IMDB_CLONE_CONCIERGE_ADDRESS` (optional; local Vite uses `/concierge-api`)
- `VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS`

These are defined in `frontend/.env.development` for local development and in
`frontend/.env.production` for production builds. Use `.env.local` or `.env.*.local` for private local
overrides; those files are ignored by Git.

Browser observability is best effort and never blocks the UI. Development defaults to a console
reporter. To exercise the same anonymous batching endpoint used in production, put
`VITE_OBSERVABILITY_CONSOLE=false` in ignored `frontend/.env.local`, run the backend, and reload the
frontend. The backend accepts at most 20 validated events per request and exports only bounded
Prometheus labels; URLs, route parameters, messages, stacks, account/session IDs, search text, and
browser fingerprints are discarded before transport.

## Run The Movie Concierge

Create the locked Python 3.14 environment, then copy the safe key template to the one exact ignored
path read by the service:

```bash
make agent-sync
mkdir -p .secrets
cp agent/movie-concierge.local.env.example .secrets/movie-concierge.local.env
chmod 600 .secrets/movie-concierge.local.env
```

Edit only `.secrets/movie-concierge.local.env` and replace the placeholder `OPENAI_API_KEY`. Do not
source, print, log, screenshot, or commit this file. A shell `OPENAI_API_KEY`, `.env`, or
`agent/.env` is not read. Configure a hard `$20` budget on the dedicated OpenAI project because it
is the authoritative limit across process restarts.

With the Java backend running, start the Luna-powered service:

```bash
make run-agent
```

The service listens on `http://localhost:8090`. Check:

```bash
curl -fsS http://localhost:8090/healthz
curl -fsS http://localhost:8090/readyz
curl -fsS http://localhost:8090/metrics
```

Production uses a separate file boundary. It never reuses a shell key: the SOPS-encrypted
`movie-concierge-runtime` Secret is mounted read-only at `/run/secrets/movie-concierge`, while local
development continues to read only `.secrets/movie-concierge.local.env`.

For deterministic UI/API development without a model key, Java, database, or search index:

```bash
make run-agent-fake
```

This fake mode must be selected explicitly. It exercises the same typed browser contract and
in-memory session behavior without pretending to validate model quality or MCP interoperability.

## Run Full Stack Locally

Use four terminals when developing all deployables:

1. Stateful services:

   ```bash
   make docker-compose-dev-up
   ```

2. Backend:

   ```bash
   ./gradlew bootRun
   ```

3. Frontend:

   ```bash
   cd frontend
   yarn start
   ```

4. Movie Concierge:

   ```bash
   make run-agent
   ```

Vite proxies `/concierge-api` to the Python service. The public launcher is available to anonymous
and authenticated users; frontend client IDs combine a stable browser ID with the current account
ID so switching accounts starts an isolated conversation. Python holds bounded history only in
memory, and a Python restart intentionally clears it.

Optional initial seed after backend startup:

```bash
make seed-local-users
make seed-light SEED_VERSION=2026-05-17
make reindex-local-search
```

Smoke checks:

```bash
curl -fsS http://localhost:8081/actuator/health
curl -fsS http://localhost:8080/v3/api-docs.yaml >/tmp/imdb-clone-openapi.yaml
curl -fsS http://localhost:9200/_cluster/health
```

Then open `http://localhost:3000`.

### Local Java MCP seam

The development profile enables the stateless Spring AI MCP endpoint at `http://localhost:8080/mcp`
with the local-only bearer value from `application-dev.properties`. The home-cluster production
endpoint is enabled only on the internal Java Service, protected by an encrypted workload token and
NetworkPolicy, and has no public ingress.

Each MCP operation is an independent JSON-RPC request. After the backend is running, inspect the
server and its generated tool schema with:

```bash
MCP_TOKEN=local-development-mcp-token

curl -fsS http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H "Authorization: Bearer ${MCP_TOKEN}" \
  --data '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"local-debugger","version":"1.0.0"}}}'

curl -fsS http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H "Authorization: Bearer ${MCP_TOKEN}" \
  --data '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
```

With PostgreSQL, OpenSearch, embeddings, seed data, and the backend ready, call a grounded tool
directly:

```bash
curl -fsS http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H "Authorization: Bearer ${MCP_TOKEN}" \
  --data '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"search_movies","arguments":{"query":"thoughtful science fiction under two hours","maxRuntimeMinutes":120,"movieType":"MOVIE","limit":3}}}'
```

The result includes MCP text content for broad client compatibility and a versioned
`structuredContent` object for the Python agent. The tool returns only compact catalog data and
never contacts PostgreSQL or OpenSearch outside the owning Java module. `tools/list` exposes exactly
`search_movies`, `get_movie_details`, `get_similar_movies`, and `get_tonight_picks`; their
annotations declare them read-only, idempotent, non-destructive, and closed-world.

## Backend Checks

Run from the repository root:

```bash
./gradlew test --tests "com.thecodinglab.imdbclone.SomeTest"
./gradlew test
./gradlew integrationTest --tests "com.thecodinglab.imdbclone.SomeIntegrationTest"
./gradlew integrationTest
./gradlew spotlessApply
./gradlew build jacocoTestReport
```

Use targeted tests while developing, then run the broader check before reporting backend work complete.
Before committing Java, Gradle, or backend test changes, run `./gradlew spotlessApply`.

The `test` task is the fast lane and excludes tests tagged `integration`. The `integrationTest` task
uses Testcontainers for PostgreSQL, OpenSearch, and RustFS where needed. Docker must be available for
`integrationTest`, `check`, and `build`. The JaCoCo report combines both test tasks.

## Frontend Checks

Run from `frontend`:

```bash
yarn install --frozen-lockfile
yarn run lint
yarn test
yarn build
yarn e2e
```

The Vite dev server is configured for port `3000` with `strictPort: true`. If port `3000` is busy,
stop the existing process or adjust the command intentionally.

Playwright tests live in `frontend/e2e` and can run by project:

```bash
cd frontend
yarn playwright test --project=desktop-chromium
yarn playwright test --project=mobile-chromium
```

## Movie Concierge Agent Checks

Run the stable full gate from the repository root:

```bash
make verify-agent
```

This includes the executable 27-case deterministic Pydantic Evals report. It checks the
machine-readable tool, argument, text, error, grounding, action, and execution expectations while
keeping qualitative review criteria explicit. Dataset-owned scenarios require no key, provider
network, Java process, or data service. Run one case while iterating with:

```bash
make eval-agent AGENT_EVAL_CASE=tonight-mode-refinement
```

Live Luna evals are deliberately absent from CI and need both the environment opt-in and the
`--live` path selected by the Make target:

```bash
IMDB_AGENT_LIVE_EVALS_ENABLED=true \
  make eval-agent-live AGENT_EVAL_CASE=exact-title-search
```

Fault-injection-only cases are excluded from live runs. Start with one case, check its `usage`
event and OpenAI project usage, and stay inside the `$20` project budget.

Use narrow checks while developing:

```bash
cd agent
uv run pytest tests/web/test_health.py
uv run pytest tests/concierge/test_eval_dataset.py
uv run ruff check src/imdb_agent/web tests/web
uv run pyright src/imdb_agent/web tests/web
uv run lint-imports
```

The full gate checks Ruff formatting and linting, strict Pyright, import contracts, architecture
tests, and all deterministic pytest tests. Live-provider evals will be separate opt-in checks.

## Generated API Client

The frontend API client is generated from the backend OpenAPI spec.

When backend API contracts change:

1. Start local services and backend.
2. Refresh the OpenAPI spec.
3. Regenerate the client.
4. Run frontend checks.

Commands:

```bash
cd frontend
yarn run updateOpenApiSpec
yarn run build:moviesGen
yarn build
```

Do not manually edit files in `frontend/src/client/movies/generator-output`.

## Docker Builds

Backend image:

```bash
make docker-build-backend
make container-smoke-backend
```

Frontend image:

```bash
make docker-build-frontend
make container-smoke-frontend
```

The frontend Dockerfile installs Java because `openapi-generator-cli` runs during the image build.
Backend and frontend runtime images use numeric non-root users and support a read-only root
filesystem. Their smoke tests run with dropped Linux capabilities and exercise the same HTTP health
paths used by Kubernetes; the backend smoke also starts the Pyroscope Java agent.

Movie Concierge image and smoke test:

```bash
make docker-build-agent
make container-smoke-agent
```

The agent image contains runtime dependencies only and runs as numeric non-root user `10001`.

Agent Prometheus metrics include bounded run outcomes/duration, first-event latency, active runs,
configured guardrail limits, known tool names, provider input/cache-read/cache-write/output tokens,
estimated USD cost, process-budget commitment, and SSE disconnects. Logs record stable failure
codes only; they do not contain prompts, bodies, tool payloads, client/conversation IDs,
authorization headers, or keys.

The backend also publishes anonymous frontend real-user metrics under `imdb_frontend_*`, while a
cluster-only ServiceMonitor scrapes llama.cpp's native embedding throughput and queue metrics. The
Grafana Frontend dashboard uses p75 for Web Vitals and the Backend, PostgreSQL, and Infrastructure
dashboards expose deeper search, pool, runtime, contention, throttling, network, and inode signals.

Production traces are sent over OTLP/HTTP from Python and Spring Boot to Alloy, then stored in
Tempo. Pydantic AI content capture and model-request serialization are disabled. Trace context is
propagated through the Python MCP HTTP client so a single trace can correlate FastAPI, model/tool,
and Java MCP work. Concrete conversation paths, query strings, client network data, and user agents
are redacted from inbound spans. Loki receives logs from all Kubernetes namespaces plus Kubernetes
Events. Pyroscope continuously samples Python CPU/allocations and Java JFR CPU/allocations/locks in
production; it is disabled by default for local runs. Use the private endpoints and DBeaver settings
in [`operations.md`](operations.md); do not expose the database or observability APIs with an ingress.

## Kubernetes And k3s Validation

Home-cluster manifests are rendered from:

```text
infrastructure/clusters/home/apps
```

The production movie seed is a separate, manually synchronized Argo Application. Its versioned Job
lives under `infrastructure/clusters/home/maintenance/movie-seed` and is never executed by an
ordinary `home-root` application release.

Render without applying:

```bash
kubectl kustomize infrastructure/clusters/home/apps >/tmp/imdb-clone-home-apps.yaml
make verify-seed-release
make verify-movie-concierge-production
make verify-observability-production
make verify-observability-charts
make verify-kubernetes-schema
```

Read-only cluster checks, when kube access is available:

```bash
kubectl get applications -n argocd
kubectl get pods -A
kubectl -n imdb-clone get deploy,svc,ingress
kubectl -n imdb-clone rollout status deploy/imdb-clone-backend
kubectl -n imdb-clone rollout status deploy/imdb-clone-frontend
```

Do not run `kubectl apply`, `kubectl delete`, `argocd app sync`, rollout restarts, or namespace/PVC
mutations unless explicitly asked. The normal app release path is the version-gated CD workflow, which
builds images and commits digest-pinned manifest updates.

## Live Search Relevance Evaluation

The ordinary unit test verifies the metric calculations. The opt-in live evaluation calls the running
backend and measures the actual OpenSearch and embedding rankings against the versioned judgements in
`src/test/resources/search/relevance-live-v1.json`.

Prepare the versioned local corpus and start the backend in a separate terminal:

```bash
make seed-light SEED_VERSION=2026-05-17
make reindex-local-search
./gradlew bootRun
```

Then run:

```bash
./gradlew liveSearchEvaluation
```

Set `IMDB_CLONE_SEARCH_BASE_URL` when evaluating a backend other than `http://localhost:8080`.
The live evaluation is intentionally not part of `check` because it depends on the seeded corpus and
the local embedding service.

Search operations expose `imdb.search.requests` and `imdb.search.duration` through Micrometer. The
metrics use bounded `mode` and `result` tags and never record the user's query text.

## Environment Variables And Secrets

Backend configuration keys live in:

- `src/main/resources/config/application.properties`
- `src/main/resources/config/application-dev.properties`
- `src/main/resources/config/application-prod.properties`

Important backend configuration areas:

- `spring.datasource.*`
- `opensearch.*`
- `spring.mail.*`
- `imdb-clone.identity.*`
- `imdb-clone.media.storage.*`
- `imdb-clone.notification.*`
- `imdb-clone.recommendation.*`

Frontend build/runtime variables:

- `VITE_IMDB_CLONE_BACKEND_ADDRESS`
- `VITE_IMDB_CLONE_CONCIERGE_ADDRESS` (optional; Vite proxies `/concierge-api` to port `8090` by
  default)
- `VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS`

Movie Concierge variables:

- `IMDB_AGENT_ENVIRONMENT` (`local`, `test`, or `production`)
- `IMDB_AGENT_VERSION`
- `IMDB_AGENT_HOST`
- `IMDB_AGENT_PORT`
- `IMDB_AGENT_MODEL_BACKEND` (`openai` by default; use `fake` only for deterministic development)
- `IMDB_AGENT_MODEL_NAME` (`gpt-5.6-luna` by default)
- `IMDB_AGENT_SECRETS_DIRECTORY` (required in production; points to read-only projected files)
- `IMDB_AGENT_MCP_BEARER_TOKEN` (local/test compatibility only; production ignores it in favor of
  the mounted file)
- `IMDB_AGENT_ALLOWED_HOSTS` (JSON list; production forbids wildcard hosts)
- `IMDB_AGENT_MAX_CONCURRENT_RUNS`, `IMDB_AGENT_MAX_CONVERSATIONS`, and
  `IMDB_AGENT_MAX_REQUEST_BODY_BYTES`
- `IMDB_AGENT_PROJECT_COST_LIMIT_USD` (cannot exceed `$20`)
- `IMDB_AGENT_RUN_COST_LIMIT_USD`, model/tool/token limits, and timeout settings
- `IMDB_AGENT_LIVE_EVALS_ENABLED` (defaults to `false` and still requires the `--live` CLI flag)

Java MCP production variables/config-tree entries:

- `movie_concierge_mcp_enabled` (defaults to `false`; the production GitOps Deployment sets it to
  `true` only with the mounted workload token)
- `movie_concierge_mcp_bearer_token` (required and non-empty whenever MCP is enabled)

Secret handling:

- Local override files such as `.env.local` and `.env.*.local` are ignored.
- `.secrets/` is ignored and must not be printed or committed.
- The Movie Concierge reads `OPENAI_API_KEY` only from the exact ignored
  `.secrets/movie-concierge.local.env` file. A shell variable with that name is intentionally
  ignored.
- Production reads the OpenAI and MCP credentials only from the SOPS-backed files beneath
  `/run/secrets/movie-concierge`; neither credential is a Python environment variable.
- Kubernetes secret manifests use SOPS/age as `*.sops.yaml`.
- `.sops.yaml` defines encryption rules for `infrastructure/clusters/home/*.sops.yaml` files.
- Do not replace encrypted SOPS content with plaintext.

## Common Troubleshooting

Port already in use:

- Frontend needs `3000` because Vite uses `strictPort: true`.
- Backend uses `8080`; management/health uses `8081`.
- Movie Concierge uses `8090`.
- PostgreSQL uses `5432`, OpenSearch uses `9200`, RustFS uses `9000` and `9001`.

Backend cannot connect to services:

- Confirm `docker compose ps` shows PostgreSQL, OpenSearch, and RustFS running.
- Confirm the backend is using the `dev` profile or default local profile behavior.
- Check `curl -fsS http://localhost:9200/_cluster/health` for OpenSearch readiness.

Frontend API calls fail:

- Confirm backend is reachable at `http://localhost:8080`.
- Confirm `frontend/.env.development` points to the expected backend and object-storage hosts.
- Regenerate the frontend client if backend API contracts changed.

Movie search returns stale or empty results:

- Seed movie data first.
- Run `make reindex-local-search` after seeding or changing catalog data.

Object images do not load:

- Confirm RustFS is running on `9000`.
- Confirm the bucket init container completed.
- Re-run the seed if media objects were not uploaded.

Testcontainers tests fail:

- Ensure Docker is running.
- Re-run the narrow failing test first.
- If containers are stale, stop Compose services only if they conflict with the test; do not delete
  volumes unless explicitly approved.

Movie Concierge environment or dependency setup fails:

- Run `make check-agent-tools` and confirm `python3 --version` and `uv run python --version` report
  Python 3.14.
- Run `make agent-sync`; do not edit `uv.lock` manually.
- Invalid settings intentionally fail with a redacted `invalid Movie Concierge configuration`
  message. Check only the relevant `IMDB_AGENT_*` values without printing secrets.

## Verification Matrix

| Area | Commands |
| --- | --- |
| Frontend install | `cd frontend && yarn install --frozen-lockfile` |
| Frontend lint | `cd frontend && yarn run lint` |
| Frontend tests | `cd frontend && yarn test` |
| Frontend build | `cd frontend && yarn build` |
| Frontend e2e | `cd frontend && yarn e2e` |
| Agent locked install | `make agent-sync` |
| Agent format/lint/type/architecture/tests | `make verify-agent` |
| Agent targeted test | `cd agent && uv run pytest tests/path/test_file.py` |
| Backend targeted fast test | `./gradlew test --tests "com.thecodinglab.imdbclone.SomeTest"` |
| Backend fast tests | `./gradlew test` |
| Backend targeted integration test | `./gradlew integrationTest --tests "com.thecodinglab.imdbclone.SomeIntegrationTest"` |
| Backend integration tests | `./gradlew integrationTest` |
| All backend tests | `./gradlew test integrationTest` |
| Backend package/check | `./gradlew build jacocoTestReport` |
| Backend formatting | `./gradlew spotlessApply` |
| Backend image | `docker build --platform linux/amd64 -t imdb-clone-backend .` |
| Frontend image | `cd frontend && docker build --platform linux/amd64 -t imdb-clone-frontend .` |
| Agent image | `make docker-build-agent` |
| Agent image smoke | `make container-smoke-agent` |
| k3s manifest render | `kubectl kustomize infrastructure/clusters/home/apps >/tmp/imdb-clone-home-apps.yaml` |
| k3s namespace status | `kubectl -n imdb-clone get deploy,svc,ingress` |
| k3s rollout status | `kubectl -n imdb-clone rollout status deploy/imdb-clone-backend` and frontend equivalent |
| API smoke | `curl -fsS http://localhost:8081/actuator/health` and `curl -fsS http://localhost:8080/v3/api-docs.yaml` |
