# Movie Concierge Product Vision

**Status:** Accepted product direction

**Last updated:** 2026-08-24

**First release:** Read-only text concierge

**Current milestone:** Production read-only pilot and grounded `open_movie` action deployed;
capability expansion is tracked in the [long-term roadmap](movie-concierge-roadmap.md)

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

The first interface is text based. A public `Ask Concierge` action opens a
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

The first prompt is a visible capability guide. Questions such as “What can you do for me?” are
answered from a stable product-owned response without calling the model or Java tools. It lists
catalog search, grounded details, similar movies, constrained Tonight Mode picks, and grounded
movie-page navigation, together with the release's read-only limitations.

A representative journey is:

1. The user describes an imprecise intent.
2. The Concierge asks one useful clarification only when the missing preference materially changes
   the result.
3. It calls Java-owned search or recommendation tools.
4. It presents a small grounded set with meaningful differences and existing explanations.
5. The user refines a constraint or opens a movie detail page.

The agent may emit a typed navigation suggestion when the user explicitly asks to open a movie. The
React application performs the navigation; navigation is not an LLM tool with arbitrary URLs.

### Grounded UI-action contract

`open_movie` is an application action, not a provider tool and not a model-generated route. The
provider-independent Concierge core emits it only after an explicit open request resolves to one
unique Java-MCP-grounded movie in the current run. Ambiguous, missing, stale, failed, or forged
grounding emits no action. The event carries only a positive catalog movie ID; React validates the
strict event, requires the same card to have appeared earlier in that stream, builds the known
movie-detail route itself, and closes the Concierge overlay.

The same typed action can later be consumed by a realtime voice adapter. Voice may therefore say
less or nothing while the existing React application executes the already-tested action contract;
it does not need a second navigation mechanism or permission model.

## Read-Only MVP

### In scope

- Capability discovery in natural language.
- Catalog search from title-like and descriptive requests.
- Movie detail lookup for factual follow-up questions.
- Similar-movie discovery through the existing recommendation capability.
- Tonight Mode choices with mood, genre, runtime, era, and exclusion constraints.
- Multi-turn refinement with bounded conversation history.
- Typed streaming text, status, movie-card, UI-action, error, usage, and completion events.
- Provider-independent agent code and deterministic model/tool fakes.
- A versioned eval set covering normal, ambiguous, adversarial, and failure cases.
- Production metrics, structured logs, privacy-safe traces, continuous profiles, token usage, and
  estimated-cost accounting.

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

- **Python service:** Python 3.14, uv, FastAPI/Uvicorn, Pydantic v2 and
  pydantic-settings, Pydantic AI 2.31.0, pytest, strict Pyright, curated Ruff rules, Import Linter,
  and enforced architecture tests. The exact framework pin keeps the Realtime surface reproducible;
  provider/framework types remain inside adapters so a future `Agent.realtime()` channel can reuse
  product policy and Java tools.
- **Java MCP server:** Spring Boot and Spring AI 2.x, stateless Streamable HTTP, Spring Security,
  safe exception mapping, and tools that call existing public module interfaces.
- **Frontend:** the existing React/TypeScript/Material UI application, an application-owned typed
  SSE client, and structured result components.
- **Models:** `gpt-5.6-luna` is the initial text model because it supports streaming and function
  calling at the cost-sensitive tier. Provider selection remains configuration, and later models
  run through the same eval set before promotion. The provider adapter is replaceable so the same
  product ports can later back a Pydantic AI realtime/voice channel.
- **Telemetry:** OpenTelemetry traces in Tempo, Prometheus metrics, structured JSON logs in Loki,
  Grafana dashboards and alerts, and provider-independent Pydantic Evals. A dedicated LLM
  observability product is optional later if its additional semantic views justify another
  datastore and a separately approved content-retention policy.

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

The production read-only pilot uses a bounded in-memory conversation repository behind an
Interface. History is limited by turns, tokens, inactivity, and a process-wide conversation count;
it is deliberately lost on restart. A future multi-replica release requires a durable Adapter with
explicit expiry, deletion, schema ownership, and retention policy. Long-term taste memory is never
inferred silently from chat transcripts.

## Evaluation Contract

The current versioned dataset contains 27 deterministic cases. Each case records the user prompt,
allowed tools, expected tool choice where deterministic, important arguments, forbidden behavior,
and grounded output expectations. It grows with every new capability and every reproducible
semantic failure.

Executable expectations and human judgment are explicit rather than conflated. Required and
forbidden tools, important arguments, required and forbidden text terms, safe error codes,
grounding, UI actions, and run limits are machine checked. `review_criteria` and `review_risks`
record qualitative judgments such as usefulness, explanation quality, and diversity. Synthetic
deterministic scenarios and movie fixtures are dataset-owned; they exercise the eval harness
without a provider key or case-ID branches in Python.

One provider-neutral internal tool-call event is the source of truth for accepted tool execution.
The model Adapter emits it only after framework validation accepts the complete call. Concierge
orchestration derives user-visible progress and bounded metrics from it, and evals consume the same
event for tool and argument assertions. Tool arguments never enter the browser event contract or
operational telemetry.

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
3. **OpenTelemetry and Tempo** show one trace per agent run with content-free model and tool spans,
   release, latency, usage, and estimated cost. W3C propagation connects FastAPI, outbound MCP, and
   Java work; Grafana links Tempo traces and Loki log entries through trace IDs.
4. **Evals** detect semantic failures that infrastructure metrics cannot see, including wrong tools,
   weak arguments, hallucinated facts, ignored constraints, and regressions between models.

Operator endpoints, private tunnels, Grafana access, and the trace-to-log incident workflow are
documented in the [production operations runbook](operations.md).

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
the repository's policy of avoiding raw queries, bodies, and user identifiers. Production export
therefore uses Pydantic AI `InstrumentationSettings` with content, binary content, tool payloads,
and serialized model-request parameters disabled. HTTP request/response bodies and authorization
headers are not captured. The low-volume pilot samples all agent traces, keeps them in the
self-hosted Tempo instance for three days, and keeps logs in Loki for seven days. Export failures
are fail-open for user requests.

A hosted or self-hosted LLM-observability product must remain opt-in until content retention,
regional processing, deletion, access control, and separate development/production projects are
explicitly approved. Self-hosting alone does not remove that privacy decision.

## Delivery History And Roadmap

| Foundation milestone | Status |
|---|---|
| M0 — Product and eval contract | Completed |
| M1 — Python deployable foundation | Completed 2026-07-16 |
| M2 — Protected Java read-only MCP Seam | Completed |
| M3 — Headless bounded Concierge core | Completed 2026-08-18 |
| M4 — React walking skeleton | Completed 2026-08-18 |
| M5 — Production guardrails and observability | Deployed |
| M6 — Four-tool read-only production pilot with grounded UI action | Deployed |

The execution records for the first two implementation slices remain in
[Movie Concierge foundation](superpowers/plans/2026-07-16-movie-concierge-foundation.md) and
[Movie Concierge MCP search](superpowers/plans/2026-07-16-movie-concierge-mcp-search.md). Future
capabilities, delegated identity, safe personal actions, external enrichment, durable scale, and
voice are ordered with measurable exit evidence in the
[Movie Concierge long-term roadmap](movie-concierge-roadmap.md).

## Open Decisions

- Review ownership and traffic-derived release thresholds as the eval dataset grows.
- The provider/model that wins the measured quality/latency/cost comparison.
- Conversation retention duration and the production store topology.
- The end-user authentication and delegated-token design between React, Python, and Java.
- Whether a future semantic trace system may retain any separately approved redacted content.
- Baseline-derived service objectives and monthly cost alert thresholds.
- Whether a later approval workflow needs Pydantic Graph or a durable execution engine.
