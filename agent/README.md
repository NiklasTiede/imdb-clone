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

The only supported credential field is `OPENAI_API_KEY`. The service reads it directly from
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

## Package map

```text
src/imdb_agent/
├── concierge/    provider-independent policy, events, ports, sessions, orchestration
├── web/          FastAPI and typed SSE inbound adapter
├── adapters/     Pydantic AI/OpenAI/MCP, memory, eval, logging, metrics, and fakes
├── bootstrap.py  composition root
└── settings.py   validated non-secret settings and exact secret-file loading
```

See [`AGENTS.md`](AGENTS.md) for Python-specific architecture rules and
[`docs/movie-concierge.md`](../docs/movie-concierge.md) for product boundaries.
