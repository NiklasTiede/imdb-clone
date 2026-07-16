# ADR 0001: Movie Concierge Architecture

**Status:** Accepted

**Date:** 2026-07-16

## Context

The application needs a conversational movie-discovery capability that demonstrates production
agent engineering without weakening the existing modular Java domain. The repository already owns
catalog retrieval, hybrid search, recommendations, account behavior, security, metrics, and k3s
deployment. The new capability must reuse those assets, support rapid experimentation in the Python
AI ecosystem, and remain diagnosable and cost-controlled in production.

The first workflow is a bounded read-only tool loop. More complex requirements—durable approvals,
personal actions, voice, and external agents—may arrive later but should not force complexity into
the first release.

## Decision

### 1. Add one Python deployable to this monorepo

Create a top-level `agent/` service targeting Python 3.14 and managed with uv. It is deployed and
versioned independently but developed beside the Java and React applications so cross-runtime
contracts and end-to-end tests can evolve atomically.

Use a modular-monolith package layout with enforced import direction. The initial conceptual
boundaries are:

```text
imdb_agent
├── concierge       product policy, orchestration interfaces, typed events
├── web             FastAPI/SSE inbound adapter
├── adapters        model, MCP, persistence, and telemetry outbound adapters
├── bootstrap.py    composition root
└── settings.py     validated environment configuration
```

Future conversation, access, or approval modules are introduced only when they own meaningful
behavior. Strict Pyright, curated Ruff rules, Pydantic validation at external seams, Import Linter,
and architecture tests keep the service navigable and its dependency direction explicit.

### 2. Use Pydantic AI for the initial agent harness

Use Pydantic AI 2.x rather than LangGraph for the first implementation. It provides the required
typed dependencies and outputs, validated tool arguments, streaming run events, MCP integration,
usage limits, model abstraction, OpenTelemetry hooks, and Pydantic Evals without requiring a graph
for a simple tool loop.

Do not hide Pydantic AI types throughout product code. The `concierge` module defines product-owned
commands, events, policies, and ports; the adapter and composition layers translate framework
types. This keeps a future framework change bounded.

Adopt an explicit graph or durable workflow engine only when branching, suspension/resumption,
parallel activities, or approval state can no longer be expressed clearly by the bounded run loop.

### 3. Keep all movie and account domain ownership in Java

The Python service never queries the Java domain PostgreSQL database or OpenSearch directly. It
does not build a parallel vector store, ranking algorithm, taste model, or authorization model.

The Spring Boot backend exposes compact tools through one centralized Spring AI MCP protocol
adapter. That adapter calls only public Spring Modulith interfaces owned by catalog,
recommendation, engagement, identity, and other domain modules. MCP schemas, workload
authentication, input validation, projections, and safe protocol error mapping stay localized in
the adapter; business decisions stay in their existing modules.

Use stateless MCP over Streamable HTTP. The initial endpoint is internal to the cluster, protected
by Spring Security and NetworkPolicy, and safe for horizontal scaling without sticky sessions.

### 4. Keep the browser protocol application-owned

React calls FastAPI through an application-owned chat contract and typed Server-Sent Events. The
event vocabulary includes text deltas, safe status updates, structured movie results, navigation
suggestions, recoverable errors, and completion metadata.

Do not make the frontend depend directly on provider events, Pydantic AI types, MCP messages, or a
generative-UI framework. The React application renders known components and performs route changes.
SSE is sufficient for the text-first server-to-browser stream; a separate input request can cancel
or continue a run. Realtime voice may use a different transport later without changing domain
tools.

### 5. Separate conversation state from business state

Python owns bounded conversation and later approval-orchestration state behind repository
interfaces. Java remains the source of truth for catalog, identity, watchlist, rating, and other
business state.

An ephemeral repository is acceptable for the local walking skeleton. Production conversation
state uses a separate schema or database, explicit expiry, bounded history, and a documented
retention policy. Chat transcripts are not silently converted into durable taste signals.

### 6. Separate operational telemetry from LLM engineering telemetry

The service exposes bounded Prometheus metrics and structured logs to the existing operational
stack. It emits OpenTelemetry-compatible traces with shared correlation context across Python, MCP,
and Java. Langfuse receives selected LLM traces, prompt versions, usage/cost data, datasets, and
scores and provides its own UI and datastore.

Grafana is not backed by Langfuse. Critical operational and cost aggregates are emitted to
Prometheus directly so alerting does not depend on the LLM trace system.

Production LLM trace export is opt-in after redaction, sampling, retention, secret filtering, and
environment separation are verified. High-cardinality identifiers never become metric labels.

### 7. Defer user mutations until delegated authorization exists

The read-only MVP uses workload authentication between Python and Java. Workload authentication
does not grant authority to act for an account and the model may never supply a trusted account ID.

Before watchlist or rating mutations, introduce a short-lived delegated actor that Java validates,
an explicit user-visible proposal, durable approval state, reauthorization immediately before
execution, idempotency, expiry, and an audit trail. Java remains the final authorization authority.

## Runtime View

```text
React / TypeScript
  └── typed chat request + SSE
      └── Python Movie Concierge
          ├── Pydantic AI → configured LLM provider
          ├── bounded conversation state
          └── MCP client → Spring MCP adapter
                            └── public Java module interfaces
                                ├── Catalog → PostgreSQL / OpenSearch
                                ├── Recommendation
                                └── later Engagement / Identity

Telemetry
  ├── Prometheus + Grafana: health, latency, errors, saturation, tokens, cost, alerts
  ├── structured log pipeline: safe correlated failure detail
  └── OpenTelemetry + Langfuse UI: run/model/tool traces, prompts, usage, scores
```

## Consequences

### Positive

- The portfolio demonstrates Python agent engineering while preserving a credible JVM domain core.
- Tool and authorization boundaries are reusable by voice and future external agents.
- Existing search and recommendation quality work remains the single source of truth.
- Typed application events prevent provider/framework details from leaking into React.
- Pydantic AI keeps the initial harness small while leaving a clear escalation path to graphs or
  durable workflows.
- Independent operational metrics and LLM traces support both alerting and semantic debugging.

### Costs and risks

- The system gains another deployable, language toolchain, container, contract seam, and failure
  mode.
- Cross-runtime authentication and trace propagation require explicit design and tests.
- SSE disconnects must cancel downstream model and tool work to avoid wasted cost.
- Model output remains nondeterministic; deterministic tests and eval gates are both required.
- LLM traces can contain substantially more sensitive content than ordinary application telemetry.
- Python 3.14 support must be proven across the locked dependency set and CI.

## Alternatives Considered

### Implement the agent in Spring AI or LangChain4j

Java could implement the feature and would reduce the number of deployables. It was rejected for
this project because demonstrating current Python agent-harness practice is an explicit portfolio
goal and Python offers the desired provider, eval, and observability integration velocity. This is
a strategic choice, not a claim that the JVM ecosystem is incapable.

### Use LangGraph from the beginning

LangGraph is strong for explicit state machines, durable branching, and resumable workflows. It is
not selected for a first bounded read-only ReAct-style loop because it would add concepts without a
current product need. Revisit it when actual workflow state justifies the graph.

### Let Python access PostgreSQL or OpenSearch

Rejected because it bypasses Java authorization, validation, ranking policy, domain interfaces,
and observability while duplicating schema and index coupling.

### Add custom Java REST endpoints instead of MCP

REST remains appropriate for browser-facing product APIs. MCP is selected for the agent tool seam
because discoverable typed tools are reusable across agent runtimes and future clients. The MCP
adapter still follows the same security and module-boundary discipline as any other inbound adapter.

### Put the Python service in a separate repository

Rejected for the current single-developer project. The monorepo allows one change to update Java
tools, Python adapters, React event consumers, tests, and deployment manifests atomically. Separate
builds and images preserve deployability.

### Start with voice or generative UI

Rejected for the MVP. Text makes tool behavior, corrections, evals, and cost easier to inspect.
Known movie cards are more predictable and accessible than arbitrary generated component trees.

## Revisit Triggers

Revisit this decision if:

- approval or long-running workflows require durable suspension and resumption;
- an external public MCP surface introduces a different security and compatibility contract;
- voice becomes the primary validated user channel;
- Python 3.14 cannot be supported by a safe locked dependency set;
- measured operational cost or reliability argues for consolidating deployables;
- regulations require a different trace store, hosting region, or retention design.
