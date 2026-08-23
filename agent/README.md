# IMDb Clone Movie Concierge

Local, read-only conversational discovery service for the IMDb Clone. It uses Pydantic AI 2.31.0
with `gpt-5.6-luna`, calls the Java domain through protected MCP tools, and streams an
application-owned event contract to React.

## Local setup

Create the locked Python 3.14 environment:

```bash
make agent-sync
```

For the Luna runtime, copy the safe template and edit only the ignored destination:

```bash
mkdir -p .secrets
cp agent/movie-concierge.local.env.example .secrets/movie-concierge.local.env
chmod 600 .secrets/movie-concierge.local.env
```

The only supported local credential field is `OPENAI_API_KEY`. The service reads it directly from
`.secrets/movie-concierge.local.env`; it does not read a shell `OPENAI_API_KEY` or a working-directory
dotenv file. Never print, source, log, screenshot, or commit the secret file. Configure a $20 hard
project budget in the OpenAI dashboard as the authoritative cross-process spending limit.

Start PostgreSQL/OpenSearch/RustFS, the Java backend, the Python service, and Vite in separate
terminals:

```bash
make docker-compose-dev-up
./gradlew bootRun
make run-agent
cd frontend && yarn start
```

Vite proxies `/concierge-api` to `http://localhost:8090`. The Java MCP endpoint is
`http://localhost:8080/mcp` and uses the local development workload token from the checked-in dev
configuration.

For UI development without Java or a provider key:

```bash
make run-agent-fake
```

The fake backend is explicit and deterministic. The normal `make run-agent` path defaults to Luna
and fails safely when the dedicated key file is absent or invalid.

## Contract and safety

The browser creates a server conversation, then POSTs messages and consumes typed server-sent
events:

- `status` — bounded user-visible progress, never private reasoning;
- `text` — incremental answer text;
- `movie-card` — a Java-tool-grounded movie projection;
- `error` — redacted stable code, safe message, and retryability;
- `usage` — requests, tool calls, input/cache/output tokens, and estimated USD cost;
- `completion` — terminal outcome and conversation identifier.

Conversation state is process-local, bounded, isolated by a browser/account client identifier, and
intentionally cleared on Python restart. Only one turn may run per conversation. Disconnects cancel
the downstream run. Model requests are bounded by time, concurrency, request, tool-call, token,
per-run cost, and process budget limits.

Movie facts are accepted only from four Java-owned tools:

- `search_movies`
- `get_movie_details`
- `get_similar_movies`
- `get_tonight_picks`

Python never reads PostgreSQL or OpenSearch. Account mutations, web search, long-term memory, and
voice are outside this local read-only release.

## Evals and verification

Run the deterministic 20-case suite and complete agent gate without a key or network:

```bash
make eval-agent
make verify-agent
```

The dataset covers normal, ambiguous, adversarial, tool-error, grounding, constraint-refinement,
budget, and unsupported-mutation behavior. Pydantic Evals checks required/allowed tools, important
arguments, catalog identifier grounding, safe errors, and run bounds. Fault-injection-only cases
remain deterministic.

Live evals require two deliberate controls and are never part of CI:

```bash
IMDB_AGENT_LIVE_EVALS_ENABLED=true \
  make eval-agent-live AGENT_EVAL_CASE=exact-title-search
```

The live path uses the same dedicated secret file, Luna limits, and Java MCP service. Start with one
case and inspect usage before expanding the selection.

Container verification stays keyless by selecting the fake backend:

```bash
make docker-build-agent
make container-smoke-agent
```

## Production pilot contract

The home-cluster pilot remains a single process with bounded, in-memory conversation state. It
stores at most 500 conversations, evicts the least-recently-used inactive session at that bound,
and intentionally loses all history on restart. Deployments use `Recreate` so two independent
session and budget ledgers never overlap; a rollout therefore has a short controlled unavailability
window. There is no agent database or user-specific data.

Production credentials come only from the SOPS-encrypted `movie-concierge-runtime` Kubernetes
Secret. Kubernetes projects two read-only files under `/run/secrets/movie-concierge`:

- `openai-api-key` for the provider Adapter;
- `mcp-bearer-token` for the protected Java workload boundary.

The Java backend receives only the MCP file, never the OpenAI credential. Production startup fails
with a redacted configuration error when either projected file is missing, invalid, or accessible
to other Unix users. Local startup continues to use the exact ignored dotenv path documented above.
Neither mode accepts an `OPENAI_API_KEY` environment variable.

The public browser contract is same-origin at `/concierge-api/v1`. Traefik strips only the
`/concierge-api` prefix and applies a dedicated per-source rate limit plus a four-request global
in-flight limit. `/healthz`, `/readyz`, and `/metrics` are not public ingress paths. The application
also enforces:

- a 4 KiB request-body limit before FastAPI parses the body;
- 600 characters per message and strict typed request models;
- two active agent runs, rejected immediately rather than queued when saturated;
- four model requests, six tool calls, 12,000 input tokens, 1,500 output tokens, 30 seconds, and
  $0.25 per run;
- a $20 process budget as a final local guard, with the provider project cap remaining authoritative
  across restarts.

Prometheus collects bounded HTTP, run, outcome, first-event latency, tool, token, provider-estimated
cost, process-budget, saturation, and disconnect metrics. The `IMDb Clone / Movie Concierge`
Grafana dashboard visualizes those signals. PrometheusRules cover availability, errors, latency,
MCP/provider failures, capacity, and cost. Alertmanager is deliberately not installed yet, so rules
are visible in Prometheus/Grafana but do not send notifications.

Validate the complete production contract without deploying:

```bash
make verify-agent
make docker-build-agent
make container-smoke-agent
make verify-movie-concierge-production
make verify-kubernetes-schema
```

Do not apply these manifests directly. The version-gated release workflow builds all three app
images, pins their Docker digests, and updates the existing GitOps tree after an intentional
`VERSION` release.

## Package map

```text
src/imdb_agent/
├── concierge/    provider-independent policy, events, ports, sessions, orchestration
├── web/          FastAPI and typed SSE inbound adapter
├── adapters/     Pydantic AI/OpenAI/MCP, memory, eval, logging, metrics, and fakes
├── bootstrap.py  composition root
└── settings.py   validated non-secret settings and environment-specific secret-file loading
```

See [`AGENTS.md`](AGENTS.md) for Python-specific architecture rules and
[`docs/movie-concierge.md`](../docs/movie-concierge.md) for product boundaries.
