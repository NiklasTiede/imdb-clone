# Movie Concierge Product Vision

**Status:** Accepted product direction

**Last updated:** 2026-07-16

**First release:** Read-only text concierge

**Current milestone:** M1 Python deployable foundation

## Vision

The Movie Concierge helps people turn vague entertainment intent into a confident movie choice
using trusted catalog data, explainable recommendations, and safe actions.

The product should be useful when a user knows only that they want “something tense but not too
long,” when they want movies similar to one they already like, or when they want to refine a choice
over several turns. It is a conversational discovery surface over the existing catalog, search,
recommendation, and account capabilities—not a second movie database or recommendation engine.

## Why Build It

The current application already supports hybrid search, similar movies, Tonight Mode, ratings,
watchlists, and taste insights. Those capabilities are valuable but require users to know which
screen and filter to use. The Concierge can translate natural language into those existing,
measured capabilities and keep constraints coherent while the user refines a decision.

It also creates a realistic portfolio example of agent harness engineering:

- a separately deployable Python service beside a stable Java domain core;
- typed model and tool boundaries rather than an unbounded chatbot;
- MCP interoperability across runtimes;
- eval-driven model and prompt changes;
- production controls for latency, reliability, privacy, and inference cost.

Browser- or operating-system-level agents may eventually own the conversational surface. The
durable assets remain useful in that future: protected domain tools, delegated authorization,
approval policies, retrieval quality, personalization, evals, and observability. The Python service
can then become a trusted server-side orchestration and policy layer or expose the same capabilities
to those clients.

## Product Principles

1. **Ground every movie claim.** Titles, identifiers, metadata, availability in this catalog, and
   recommendation explanations must come from Java-owned tools.
2. **Let the domain system decide.** Search retrieval, ranking, diversity, taste calculations,
   authorization, and mutations stay in the Spring Boot modules that own them.
3. **Use conversation where it reduces effort.** Normal browsing remains first class; the
   Concierge should not replace effective search, detail, or discovery screens.
4. **Make actions visible and reversible.** Read-only capabilities come first. Later account
   mutations require a visible proposal, explicit approval, reauthorization, and idempotency.
5. **Treat quality and cost as product behavior.** Every release is evaluated for tool use,
   groundedness, usefulness, latency, reliability, and cost—not only whether it produces fluent
   text.
6. **Keep the interaction channel replaceable.** Text, voice, and future external agents should use
   the same tools, policies, events, and eval contract.

## First User Experience

The first interface is text based. A `Movie Concierge` action in the application header opens a
right-side drawer on desktop and a full-screen panel on small screens. It contains a short
capability introduction and suggested prompts such as:

- “Find a clever science-fiction movie under two hours.”
- “I liked Arrival, but tonight I want something lighter.”
- “Give me three tense movies and explain the differences.”
- “What can you help me do?”

The response streams through typed events. The UI can show concise states such as `Thinking`,
`Searching the catalog`, and `Comparing three movies` without exposing private chain-of-thought.
Movie results render with existing poster-card and navigation patterns, not as Markdown links
invented by the model.

A representative journey is:

1. The user describes an imprecise intent.
2. The Concierge asks one useful clarification only when the missing preference materially changes
   the result.
3. It calls Java-owned search or recommendation tools.
4. It presents a small grounded set with meaningful differences and existing explanations.
5. The user refines a constraint or opens a movie detail page.

The agent may emit a typed navigation suggestion when the user explicitly asks to open a movie. The
React application performs the navigation; navigation is not an LLM tool with arbitrary URLs.

## Read-Only MVP

### In scope

- Capability discovery in natural language.
- Catalog search from title-like and descriptive requests.
- Movie detail lookup for factual follow-up questions.
- Similar-movie discovery through the existing recommendation capability.
- Tonight Mode choices with mood, genre, runtime, era, and exclusion constraints.
- Multi-turn refinement with bounded conversation history.
- Typed streaming text, status, movie-card, error, and completion events.
- Provider-independent agent code and deterministic model/tool fakes.
- A versioned eval set covering normal, ambiguous, adversarial, and failure cases.
- Local metrics, structured logs, trace hooks, token usage, and estimated-cost accounting before
  production exporters are enabled.

### Explicitly out of scope

- Adding, removing, or changing watchlist entries or ratings.
- Long-term personal memory or autonomous background work.
- Direct access from Python to the domain PostgreSQL database or OpenSearch index.
- A second vector store, embedding pipeline, reranker, or recommendation algorithm in Python.
- Unrestricted web search for movie facts.
- Voice or realtime audio.
- Model-generated arbitrary UI components.
- A multi-agent system or complex workflow graph without a demonstrated need.

## Capability Contract

The first Java MCP tool set is intentionally small and read only:

| Tool | User outcome | Domain owner |
|---|---|---|
| `search_movies` | Find catalog movies from title or descriptive intent | Catalog |
| `get_movie_details` | Answer grounded follow-up questions about one movie | Catalog |
| `get_similar_movies` | Discover explainable alternatives to a known movie | Recommendation |
| `get_tonight_picks` | Return a small, constrained, diverse choice set | Recommendation |

Tool results use compact, versioned projections designed for model consumption. The MCP adapter
wraps public Java interfaces and contains protocol mapping only. It does not reproduce domain logic.

Later account tools are a separate trust milestone. A workload bearer token proves that the Python
service may call MCP; it does not identify or authorize the end user. User-scoped actions require a
short-lived delegated identity that Java verifies independently.

## Technology Direction

The load-bearing architecture is recorded in
[ADR 0001](adr/0001-movie-concierge-architecture.md). The intended stack is:

- **Python service:** Python 3.14 target, uv, FastAPI/Uvicorn, Pydantic v2 and
  pydantic-settings, Pydantic AI 2.x, pytest, strict Pyright, curated Ruff rules, Import Linter, and
  a small architecture test.
- **Java MCP server:** Spring Boot and Spring AI 2.x, stateless Streamable HTTP, Spring Security,
  safe exception mapping, and tools that call existing public module interfaces.
- **Frontend:** the existing React/TypeScript/Material UI application, an application-owned typed
  SSE client, and structured result components.
- **Models:** provider-selected through configuration. The initial primary and challenger are run
  through the same eval set before either is promoted. Model names, prices, and context limits are
  deployment configuration rather than architecture decisions.
- **Telemetry:** OpenTelemetry-compatible traces, Prometheus metrics, structured JSON logs,
  Grafana dashboards and alerts, and Langfuse for LLM traces, prompt versions, datasets, and scores.

Pydantic AI is the initial orchestration framework because the first product is a bounded tool-using
agent with typed dependencies, outputs, streaming events, usage limits, MCP support, and code-first
evals. An explicit graph is added only if durable branching, resumable approvals, or multi-actor
workflows make the simpler run loop difficult to reason about.

Python 3.14 compatibility must be proven by the locked dependency set and CI in the foundation
milestone. A dependency compatibility problem is surfaced as a decision; the project must not
silently downgrade the runtime.

## State And Memory

The Python service owns conversation orchestration state. Java continues to own movie and account
business state.

The local walking skeleton may use an ephemeral conversation repository behind an interface. The
production read-only MVP uses a bounded server-side conversation store with expiry and a separate
schema or database from the Java domain data. History is limited by turns and token budget; older
content is summarized or discarded deliberately. Long-term taste memory is not inferred from chat
transcripts.

## Evaluation Contract

Milestone 0 creates an initial 10–20-case dataset before model integration; it grows to at least
20–30 cases before the production MVP. Each case records the user prompt, allowed tools, expected
tool choice where deterministic, important arguments, forbidden behavior, and grounded output
expectations.

The dataset covers:

- exact-title and descriptive discovery;
- multi-constraint requests and multi-turn refinement;
- ambiguous requests that should trigger clarification;
- details, similar movies, and Tonight Mode routing;
- no-result and downstream-tool failures;
- prompt injection attempts and requests for unsupported actions;
- attempts to invent movies, metadata, or account state;
- tool-loop, token-budget, timeout, and cancellation behavior.

Quality reporting combines deterministic assertions and reviewed judgments:

- valid tool selection and arguments;
- grounded catalog identifiers and factual consistency;
- constraint satisfaction;
- usefulness and diversity of the final choice set;
- unsupported-action refusal quality;
- task completion rate;
- end-to-end latency, time to first useful event, tokens, and estimated cost per successful task.

Initial thresholds are set only after a measured baseline. A model or prompt change must not be
promoted merely because its prose sounds better; regressions in tool accuracy, safety, latency, or
cost remain visible in the comparison.

## Production Observability

No single telemetry system is sufficient for an agent. The production debugging path combines four
views:

1. **Prometheus and Grafana** answer whether the service is healthy and alert on bounded aggregate
   signals: traffic, errors, saturation, latency, token volume, estimated cost, provider failures,
   tool failures, budget rejections, and SSE disconnects.
2. **Structured logs** explain application and integration failures using correlation IDs and safe
   error codes. They never contain secrets, authorization headers, raw prompts, or unrestricted tool
   payloads.
3. **OpenTelemetry and Langfuse** show one trace per agent run with model and tool spans, prompt
   version, release, latency, usage, estimated cost, and eval scores. Langfuse has its own UI and
   datastore; it is not the backing store for Grafana.
4. **Evals** detect semantic failures that infrastructure metrics cannot see, including wrong tools,
   weak arguments, hallucinated facts, ignored constraints, and regressions between models.

Required bounded metrics include:

- run count, success/error/budget outcome, duration, time to first event, and active runs;
- model request count, provider errors, rate limits, retries, timeouts, and cancellations;
- input, output, cached, and reasoning tokens when the provider reports them;
- estimated cost total, per run, and per successful task;
- tool call count, validation failures, duration, timeout, and result outcome by bounded tool name;
- SSE connections, disconnects, and cancellation completion;
- eval pass rate by dataset and release, kept out of request-path high-cardinality labels.

Account IDs, conversation IDs, movie IDs, prompts, and trace IDs are not Prometheus labels. Safe
correlation identifiers belong in logs and traces only.

Cost accounting uses provider-reported usage and cost when available. Otherwise, the service
calculates an estimate from input, output, cached, and other billed token classes using a versioned
price configuration. Every estimate records its model and price-table version in the trace so a
pricing change does not rewrite history silently. Dashboard totals are compared periodically with
the provider bill; they are operational estimates, not financial ledgers.

The production dashboard should make the current state obvious at a glance: runs and successful
decisions, error and cancellation rate, p50/p95 run and tool latency, time to first event, active
runs, provider retries/rate limits, token volume, cost today/7 days/current month, cost per
successful task, and model/release comparison. Baseline-derived alerts cover sustained error or
latency regressions, tool/MCP failures, provider throttling, exhausted budgets, abnormal token use,
and daily or monthly cost thresholds. Exact alert thresholds are set from canary measurements,
rather than invented before traffic exists.

### Privacy boundary

Default LLM tracing often captures prompts, tool arguments, and tool results, which conflicts with
the repository's existing policy of avoiding raw queries, bodies, and user identifiers. Production
trace export remains disabled until field-level redaction, sampling, retention, deletion, separate
development/production projects, and secret filtering are explicitly configured and tested.
Self-hosting does not remove this privacy decision.

Langfuse Cloud in an appropriate EU region is the preferred first production experiment because a
self-hosted Langfuse stack adds substantial stateful infrastructure. Self-hosting can be revisited
when data-governance requirements and cluster capacity justify it.

## Milestones

### M0 — Product and eval contract

Accept this vision and ADR, create the initial eval cases, define prohibited behavior, and record
baseline latency/cost questions. **Exit:** a framework-independent acceptance contract exists.

### M1 — Python deployable foundation

Create the top-level `agent/` project with the strict toolchain, modular package boundaries,
FastAPI health/readiness/metrics endpoints, structured logging, deterministic tests, a non-root
image, Make targets, and CI. **Exit:** the empty service is reproducibly buildable and observable
without Java, React, an LLM key, or Kubernetes. The execution-ready plan is
[Movie Concierge foundation](superpowers/plans/2026-07-16-movie-concierge-foundation.md).

**Status:** Ready for implementation.

### M2 — Java read-only MCP seam

Add one protected, stateless MCP adapter and expose the smallest useful public catalog search
interface. Start with `search_movies`, then add details, similar movies, and Tonight Mode through
existing domain interfaces. **Exit:** MCP contract and security tests prove that Python needs no
direct data-store access.

### M3 — Headless Concierge core

Add the Pydantic AI agent, MCP client, provider adapter, typed events, bounded run policy,
deterministic model/MCP fakes, and an eval command. **Exit:** a local `curl` request can stream a
grounded structured search response through Java.

### M4 — React walking skeleton

Add the header launcher, responsive text panel, typed SSE client, concise progress states, and one
structured movie-result component using existing UI primitives. **Exit:** one happy path and one
failure path work end to end in the browser.

### M5 — Production guardrails

Add workload authentication, rate/concurrency/usage/tool-loop limits, timeouts, disconnect
cancellation, trace propagation, redaction controls, image release/CD, k3s resources, NetworkPolicy,
ServiceMonitor, dashboards, and alerts. **Exit:** a deliberately constrained canary can run in k3s
with costs and failures visible.

### M6 — Read-only production MVP

Complete the four read-only tools, multi-turn constraint refinement, bounded durable history,
structured comparisons, capability discovery, and the production eval regression gate. **Exit:** a
user can reliably reach a confident movie choice, and an operator can diagnose quality, latency,
tool, provider, and cost failures.

### M7 — Personal actions

Add delegated user identity and durable proposal/approval/reauthorization/idempotency behavior
before watchlist or rating tools. **Exit:** mutations are attributable, reviewable, safe to retry,
and authorized again at execution time.

### M8 — Voice and external-agent channels

Evaluate realtime voice after the text product demonstrates repeated value. Reuse the same tools,
policies, events, approval model, and evals. **Exit:** voice is an adapter to the product rather than
a parallel agent implementation.

The **local MVP** is reached after M4 for one end-to-end search journey. The **read-only product MVP**
is reached after M6. Each milestone should land as small, cohesive commits on `master`; no feature
branch is required for the current single-developer workflow.

## Open Decisions

- Exact initial eval prompts, reviewers, and release thresholds.
- The provider/model that wins the measured quality/latency/cost comparison.
- Conversation retention duration and the production store topology.
- The end-user authentication and delegated-token design between React, Python, and Java.
- Production trace sampling and whether any redacted prompt content may be retained.
- Baseline-derived service objectives and monthly cost alert thresholds.
- Whether a later approval workflow needs Pydantic Graph or a durable execution engine.
