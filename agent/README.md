# IMDb Clone Movie Concierge

Production read-only pilot for conversational discovery in the IMDb Clone. It uses Pydantic AI
2.31.0 with `gpt-5.6-luna`, calls the Java domain through protected MCP tools, and streams an
application-owned event contract to React.

The accepted product and trust boundaries live in
[`docs/movie-concierge.md`](../docs/movie-concierge.md). Ordered work toward personal actions,
external enrichment, durable scale, and voice lives in the
[`Movie Concierge long-term roadmap`](../docs/movie-concierge-roadmap.md). This README documents
only the currently implemented runtime.

## How the agent works

The Movie Concierge is more than the language model. React owns the user experience, the Python
service owns orchestration and safety policy, the model provider proposes text and tool calls, and
Java remains the authority for movie data and recommendations. The colors below mark those system
and trust boundaries; notably, Python and the model provider have no direct data-store access.

```mermaid
flowchart TB
    subgraph reactBoundary["React browser"]
        reactUi["Concierge UI"]
        appRouter["Action validator and app router"]
    end

    subgraph pythonBoundary["Python agent service"]
        fastApi["FastAPI and typed SSE"]
        conciergeCore["Concierge policy and orchestration"]
        modelAdapter["Pydantic AI adapter"]
        toolCallEvent["Validated ToolCallEvent"]
        evidence["Bounded metrics and eval evidence"]
    end

    subgraph providerBoundary["Model provider"]
        luna["gpt-5.6-luna"]
    end

    subgraph javaBoundary["Java backend"]
        mcpBoundary["Protected MCP boundary"]
        movieCapabilities["Catalog and recommendation capabilities"]
    end

    subgraph dataBoundary["Java-owned data"]
        postgres["PostgreSQL"]
        openSearch["OpenSearch"]
    end

    reactUi -->|"Message and conversation ID"| fastApi
    fastApi -->|"Run request"| conciergeCore
    conciergeCore -->|"Bounded prompt and history"| modelAdapter
    modelAdapter -.->|"Model request"| luna
    luna -.->|"Text or proposed tool call"| modelAdapter
    modelAdapter -->|"Protected MCP request"| mcpBoundary
    mcpBoundary -->|"Named Java interface"| movieCapabilities
    movieCapabilities -->|"Catalog authority"| postgres
    movieCapabilities -->|"Search projection"| openSearch
    modelAdapter -->|"Accepted tool request"| toolCallEvent
    toolCallEvent -->|"Status and accounting"| conciergeCore
    toolCallEvent -->|"Tool and argument assertions"| evidence
    modelAdapter -->|"Grounded cards, text, usage"| conciergeCore
    conciergeCore -->|"Typed browser events"| fastApi
    fastApi -.->|"SSE stream"| reactUi
    reactUi -->|"Grounded open_movie"| appRouter

    classDef reactNode fill:#1d4ed8,color:#f8fafc,stroke:#93c5fd,stroke-width:2px
    classDef pythonNode fill:#854d0e,color:#fefce8,stroke:#fde047,stroke-width:2px
    classDef providerNode fill:#6b21a8,color:#faf5ff,stroke:#d8b4fe,stroke-width:2px
    classDef javaNode fill:#9a3412,color:#fff7ed,stroke:#fdba74,stroke-width:2px
    classDef dataNode fill:#334155,color:#f8fafc,stroke:#cbd5e1,stroke-width:2px

    class reactUi,appRouter reactNode
    class fastApi,conciergeCore,modelAdapter,toolCallEvent,evidence pythonNode
    class luna providerNode
    class mcpBoundary,movieCapabilities javaNode
    class postgres,openSearch dataNode

    style reactBoundary fill:#eff6ff,stroke:#2563eb,stroke-width:2px,color:#172554
    style pythonBoundary fill:#fefce8,stroke:#ca8a04,stroke-width:2px,color:#422006
    style providerBoundary fill:#faf5ff,stroke:#9333ea,stroke-width:2px,color:#3b0764
    style javaBoundary fill:#fff7ed,stroke:#ea580c,stroke-width:2px,color:#431407
    style dataBoundary fill:#f8fafc,stroke:#64748b,stroke-width:2px,color:#0f172a
```

A grounded request such as **“Open Arrival”** runs through a bounded agent loop. On every model
request, Luna decides whether the current context is sufficient for a final answer or whether it
needs one or more Java-owned tools. Pydantic AI validates proposed calls structurally, executes
accepted calls through MCP, and adds their typed results to the next model request. The loop stops
on a final answer, a safe failure, or a configured request, tool, token, time, or cost limit.

```mermaid
sequenceDiagram
    title Bounded grounded agent loop
    actor User
    box rgb(219, 234, 254) React browser
        participant ReactUI
    end
    box rgb(254, 249, 195) Python agent service
        participant FastAPI
        participant ConciergeCore
        participant PydanticAI
    end
    box rgb(243, 232, 255) Model provider
        participant Luna
    end
    box rgb(255, 237, 213) Java backend
        participant MCP
        participant MovieDomain
    end

    User->>ReactUI: Open Arrival
    ReactUI->>FastAPI: POST message and session
    FastAPI->>ConciergeCore: Start bounded turn
    ConciergeCore->>PydanticAI: Prompt and limited history
    loop Until final answer or configured limit
        PydanticAI->>Luna: Request with current context
        alt Context is sufficient
            Luna-->>PydanticAI: Final grounded answer
        else More catalog information is needed
            Luna-->>PydanticAI: Proposed tool call
            PydanticAI-->>ConciergeCore: Validated ToolCallEvent
            ConciergeCore-->>FastAPI: Catalog progress status
            FastAPI-->>ReactUI: SSE status
            PydanticAI->>MCP: Protected tool call
            MCP->>MovieDomain: Invoke named capability
            MovieDomain-->>MCP: Grounded movie data
            MCP-->>PydanticAI: Typed tool result
            PydanticAI-->>ConciergeCore: Grounded movie cards
        end
    end
    alt Final answer produced
        PydanticAI-->>ConciergeCore: Final text and usage
        ConciergeCore-->>FastAPI: Cards, text, usage and optional open_movie
    else Limit or dependency failure
        PydanticAI-->>ConciergeCore: Safe typed failure
        ConciergeCore-->>FastAPI: Error and completion
    end
    FastAPI-->>ReactUI: SSE stream completion
    ReactUI-->>User: Render grounded result or safe error
```

The model therefore chooses the next semantic step, but it does not control the loop without
limits. The current production defaults allow at most four model requests and six tool calls in a
30-second run, alongside input/output-token and per-run cost limits. Even after a successful model
answer, the provider-independent Concierge policy emits `open_movie` only when the current run has
resolved exactly one matching grounded movie; the model cannot supply a URL or route.

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
- `ui-action` — a strictly typed `open_movie` action containing only a grounded catalog movie ID;
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
voice are outside the current read-only release.

The model runner cannot emit UI actions directly. After a successful run, provider-independent
policy requires explicit open intent, one uniquely resolved current-run movie, and a matching
title, catalog ID, or unambiguous reference. React validates the strict action, requires the
matching card to have appeared earlier in the same stream, and builds `/movie?id=...` through an
app-owned route helper. URLs, routes, stale conversation cards, tool failures, and ambiguous results
never navigate.

## Evals and verification

Run the deterministic 27-case suite and complete agent gate without a key or network:

```bash
make eval-agent
make verify-agent
```

The dataset covers normal, ambiguous, adversarial, tool-error, grounding, constraint-refinement,
budget, UI-action, and unsupported-mutation behavior. Pydantic Evals checks required/allowed tools,
important arguments, required and forbidden text terms, exact safe error codes, catalog identifier
and UI-action grounding, and run bounds. Qualitative `review_criteria` and `review_risks` remain
explicitly human-reviewed rather than pretending to be executable assertions. Deterministic
scenarios and movie fixtures live in the dataset, so extending it does not require adding case-ID
branches to Python. Fault-injection-only cases remain deterministic.

The Pydantic AI Adapter translates every framework-validated tool call into an internal,
provider-neutral `ToolCallEvent`. The Concierge uses that single event for user-visible status and
bounded metrics, while the eval harness uses it for tool and argument assertions. Internal tool
arguments are never part of the browser SSE contract, logs, traces, or Prometheus labels.

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

Prometheus collects bounded HTTP, run, outcome, first-event latency, tool, UI-action decision,
token, provider-estimated cost, process-budget, saturation, and disconnect metrics. The
`IMDb Clone / Movie Concierge` Grafana dashboard visualizes those signals. PrometheusRules cover
availability, errors, latency,
MCP/provider failures, capacity, and cost. Alertmanager is deliberately not installed yet, so rules
are visible in Prometheus/Grafana but do not send notifications.

Production also exports one OpenTelemetry trace per sampled HTTP/agent run through the internal
Alloy OTLP endpoint to Tempo. Pydantic AI contributes child spans for model requests and tool calls;
HTTPX propagates W3C trace context across the protected MCP request, and Spring Boot continues the
trace in Java. `InstrumentationSettings` disables prompts, completions, binary content, tool
arguments/results, and serialized model request parameters. FastAPI instrumentation does not
capture request or response bodies; it replaces concrete conversation paths, query strings, client
addresses, ports, and user agents with bounded route metadata. Agent logs add `trace_id` and
`span_id` only while a valid span is active, allowing Grafana to move between Loki logs and Tempo
traces.

Production also enables continuous profiling against the private Pyroscope service. The Python
runtime samples CPU at 50 Hz and allocations at an average 512 KiB interval, uploading every 15
seconds. It does not attach PID, thread identity, prompts, request data, conversation identifiers,
or tool content. `pyroscope-otel` associates root OpenTelemetry spans with profile samples so
Grafana can move from a trace to the relevant CPU profile. Profiling is disabled by default outside
production and initialization/export failures do not break concierge requests. Production defaults
to the cluster-local endpoint so the manifest stays compatible with the previously published agent
image during the two-step image release; `IMDB_AGENT_PROFILING_ENABLED=false` is the kill switch.

Allocation profiles show which stacks allocate memory; they are not a retained-heap measurement.
Use Prometheus process metrics to answer how much memory the pod currently consumes.

The agent samples all traces during this low-volume pilot. Tempo retains them for three days and
Loki retains structured logs for seven days. Tempo, Loki, and Pyroscope remain cluster-internal;
see [`docs/operations.md`](../docs/operations.md) for private operator access.

Validate the complete production contract without deploying:

```bash
make verify-agent
make docker-build-agent
make container-smoke-agent
make verify-movie-concierge-production
make verify-observability-production
make verify-observability-charts
make verify-kubernetes-schema
```

Do not apply these manifests directly. The version-gated release workflow builds all three app
images, pins their Docker digests, and updates the existing GitOps tree after an intentional
`VERSION` release.

## Package map

```text
src/imdb_agent/
├── concierge/    provider-independent policy, tools, events, eval contract, ports, orchestration
├── web/          FastAPI and typed SSE inbound adapter
├── adapters/     Pydantic AI/OpenAI/MCP, memory, eval, logging, metrics, and fakes
├── bootstrap.py  composition root
└── settings.py   validated non-secret settings and environment-specific secret-file loading
```

See [`AGENTS.md`](AGENTS.md) for Python-specific architecture rules,
[`docs/movie-concierge.md`](../docs/movie-concierge.md) for product boundaries, and the
[`long-term roadmap`](../docs/movie-concierge-roadmap.md) for ordered capability work.
