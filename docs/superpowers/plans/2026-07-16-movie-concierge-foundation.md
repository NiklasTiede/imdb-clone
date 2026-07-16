# Movie Concierge Python Foundation Implementation Plan

**Status:** Implemented on 2026-07-16

**Milestone:** M1 — Python deployable foundation

**Product vision:** [Movie Concierge](../../movie-concierge.md)

**Architecture:** [ADR 0001](../../adr/0001-movie-concierge-architecture.md)

## Goal

Create a reproducible, strictly checked Python service that can become the Movie Concierge without
requiring the Java backend, React frontend, an LLM API key, or Kubernetes to build and test.

This slice establishes the service boundary, dependency rules, developer feedback loop, health
surface, telemetry seam, eval fixture format, and container. It intentionally does not implement an
LLM run or MCP call.

## Architecture

Create a top-level `agent/` deployable with a `src` layout. Product-owned types and policies live in
`concierge`; FastAPI is an inbound `web` adapter; model, MCP, persistence, and telemetry
implementations will live in `adapters`; `bootstrap.py` is the composition root. Enforce those
directions with static configuration and an architecture test.

Use interfaces that coding agents and CI can execute consistently:

```text
make agent-sync
make run-agent
make verify-agent
make docker-build-agent
```

`make verify-agent` is the stable aggregate interface. Native commands remain available inside
`agent/` for tight feedback.

## Technology

- Python 3.14 target, recorded in `.python-version` and CI
- uv with a committed lock file
- FastAPI and Uvicorn
- Pydantic v2 and pydantic-settings
- structlog JSON logging
- Prometheus FastAPI instrumentation
- Ruff formatting and linting
- Pyright `strict`
- pytest and HTTPX test client
- Import Linter plus a focused architecture test

Pydantic AI and provider SDKs enter in M3, when a headless agent behavior requires them. Keeping
them out of this foundation makes the first dependency and container baseline honest.

## Non-Goals

- Java MCP server changes
- provider credentials or live model calls
- Pydantic AI orchestration
- conversation persistence
- React UI or SSE chat behavior
- Kubernetes manifests or production telemetry exporters
- Langfuse deployment

## Definition Of Done

- A fresh checkout can install the locked environment and run `make verify-agent`.
- Python 3.14 executes the full dependency and test set in CI.
- Formatting, linting, strict type checking, import contracts, and tests fail independently and are
  included in the aggregate gate.
- `/healthz`, `/readyz`, and `/metrics` work without external services.
- Invalid required settings fail with a concise validation error that does not reveal secrets.
- The image builds, runs as a non-root user, and passes a health smoke check.
- Tests need no network access, API key, Java process, database, or search index.
- Root and nested agent instructions tell coding agents exactly which narrow and full commands to
  run.

## Task 1: Scaffold The Locked Python Project

**Files:**

- Create: `agent/.python-version`
- Create: `agent/pyproject.toml`
- Create: `agent/uv.lock`
- Create: `agent/README.md`
- Create: `agent/AGENTS.md`
- Create: `agent/src/imdb_agent/__init__.py`
- Create: `agent/src/imdb_agent/py.typed`
- Create: `agent/src/imdb_agent/concierge/__init__.py`
- Create: `agent/src/imdb_agent/web/__init__.py`
- Create: `agent/src/imdb_agent/adapters/__init__.py`
- Modify: `.gitignore`

### Step 1: Prove the runtime before configuring the project

Confirm uv can provision Python 3.14 and that the selected runtime and development dependencies
publish compatible wheels. If one does not, record the exact blocker and amend ADR 0001 before
changing the target. Do not silently use the system Python or relax the version.

### Step 2: Define package metadata and dependency groups

Configure a non-publishable application package with `requires-python = ">=3.14,<3.15"`, a `src`
layout, and direct runtime dependencies only. Put test, lint, type, and architecture tools in a
development group.

Keep version constraints intentional but let `uv.lock` own the complete resolved graph. Do not use
unbounded prereleases. Add a short comment only where a compatibility constraint is otherwise
surprising.

### Step 3: Configure strict feedback

In `pyproject.toml`:

- set Ruff's target version to Python 3.14;
- enable a curated rule set covering correctness, imports, async misuse, security footguns,
  comprehensions, simplification, and modernization;
- do not enable every Ruff rule blindly;
- configure Ruff formatting and import ordering;
- configure Pyright with `typeCheckingMode = "strict"`, the `src` include path, and no blanket
  unknown-type suppressions;
- configure pytest for strict asyncio behavior, concise output, and test discovery under `tests`;
- configure coverage for `imdb_agent` without imposing an arbitrary percentage before behavior
  exists;
- define Import Linter contracts for the package dependency direction.

Pydantic strict mode applies to external request, settings, event, and adapter models. Do not force
strict validation onto internal values that have already crossed a validated seam.

### Step 4: Add local instructions

`agent/AGENTS.md` owns Python-local guidance:

- working directory and Python/uv commands;
- package map and allowed import direction;
- MCP-only access to Java domain behavior;
- no direct domain PostgreSQL or OpenSearch access;
- provider-independent tests and eval fixture ownership;
- secret, logging, and trace rules;
- narrow pytest examples and the full `make verify-agent` gate.

Update the root `AGENTS.md` to route agents explicitly: read `agent/AGENTS.md` before changing
`agent/`, and read every applicable nested guide for cross-deployable changes. Do not copy the full
root guide into the nested file.

### Step 5: Lock and verify installation

Run:

```bash
cd agent
uv lock
uv sync --locked --all-groups
uv run python --version
```

Expected: the reported interpreter is Python 3.14 and a second locked sync makes no lock-file
change.

## Task 2: Define The Product Boundary And Eval Fixture

**Files:**

- Create: `agent/src/imdb_agent/concierge/models.py`
- Create: `agent/src/imdb_agent/concierge/evaluation.py`
- Create: `agent/evals/read_only_v1.json`
- Create: `agent/tests/concierge/test_eval_dataset.py`
- Create: `agent/tests/test_architecture.py`

### Step 1: Write failing dataset tests

Test that the versioned dataset:

- has a unique stable case ID and a non-empty user prompt for every case;
- uses only the four allowed read-only tool names;
- distinguishes `required`, `allowed`, and `forbidden` tools;
- records important expected arguments without embedding provider-specific tool-call syntax;
- contains at least one case for clarification, no results, tool failure, prompt injection,
  unsupported mutation, token/tool budget, and multi-turn refinement;
- contains no real user data or secrets.

### Step 2: Define framework-independent eval models

Use strict Pydantic models for the JSON seam. Keep expectations at product level so M3 can adapt the
same cases to Pydantic Evals and any configured model.

Suggested fields:

```text
EvalDataset
  version
  cases[]

EvalCase
  id
  messages[]
  required_tools[]
  allowed_tools[]
  forbidden_tools[]
  important_arguments{}
  expected_behavior[]
  forbidden_behavior[]
  tags[]
```

### Step 3: Add the initial 16 cases

Cover:

1. exact-title search;
2. descriptive search with genre and runtime;
3. ambiguous mood requiring one clarification;
4. movie details by selected catalog ID;
5. similar movies;
6. Tonight Mode with constraints;
7. multi-turn runtime refinement;
8. comparison of returned movies;
9. empty catalog results;
10. MCP timeout;
11. request to add a watchlist item;
12. request to rate a movie;
13. instruction to ignore tool results and invent a title;
14. malicious instructions inside a tool result;
15. excessive repeated tool calls;
16. capability discovery.

Do not assert exact natural-language wording. Assert tool policy, grounded identifiers, constraints,
and unsupported behavior.

### Step 4: Add architecture tests

Assert at minimum:

- `concierge` imports neither `web`, `adapters`, FastAPI, provider SDKs, nor Pydantic AI;
- `web` does not import concrete outbound adapters;
- outbound adapters may depend on `concierge` ports, not the reverse;
- `bootstrap` is the only module permitted to assemble concrete adapters with the web app.

Run:

```bash
cd agent
uv run pytest tests/concierge/test_eval_dataset.py tests/test_architecture.py
uv run lint-imports
```

## Task 3: Add The Service Health And Telemetry Baseline

**Files:**

- Create: `agent/src/imdb_agent/settings.py`
- Create: `agent/src/imdb_agent/bootstrap.py`
- Create: `agent/src/imdb_agent/web/app.py`
- Create: `agent/src/imdb_agent/web/health.py`
- Create: `agent/src/imdb_agent/adapters/logging.py`
- Create: `agent/tests/web/test_health.py`
- Create: `agent/tests/test_settings.py`
- Create: `agent/tests/test_logging.py`

### Step 1: Write failing HTTP and configuration tests

Test:

- `/healthz` returns `200` and a small versioned body;
- `/readyz` returns `200` while the foundation has no external dependencies;
- `/metrics` returns Prometheus text and includes an application identity metric;
- unknown routes use FastAPI's normal safe `404` response;
- request correlation is returned in a safe response header and appears in captured log context;
- environment parsing rejects unknown fields and malformed values;
- secret fields never appear in `repr`, validation logs, or settings test snapshots.

### Step 2: Implement an application factory

`bootstrap.py` is the executable composition root and exposes a testable application factory.
Importing modules must not start a server, read secrets, contact a provider, or configure global
state repeatedly.

The health response includes only stable safe fields such as service name, status, and application
version. Readiness is structured so M2/M3 can add MCP/provider dependency checks without changing
the route contract.

### Step 3: Configure safe structured logging

Use structlog with JSON output outside local development and readable console output locally. Bind
service, release, environment, request ID, and trace correlation where available. Define an
allow-list of safe fields; do not serialize arbitrary request objects, exceptions, settings, prompts,
headers, or tool payloads.

### Step 4: Instrument bounded HTTP metrics

Expose `/metrics` and measure request count, duration, and in-progress requests with low-cardinality
route, method, and status labels. Exclude `/metrics` from self-observation if it would distort
traffic. Do not add request IDs, paths containing IDs, clients, users, or raw exceptions as labels.

Run:

```bash
cd agent
uv run pytest tests/web/test_health.py tests/test_settings.py tests/test_logging.py
```

## Task 4: Add The Container Boundary

**Files:**

- Create: `agent/Dockerfile`
- Create: `agent/.dockerignore`
- Create: `agent/tests/container/smoke.sh`

### Step 1: Build a reproducible multi-stage image

Use uv in the builder, install from the committed lock file, and copy only the virtual environment
and application source required at runtime into a slim final image. Do not include development
dependencies, caches, tests, eval data, Git metadata, or credentials in the runtime image.

Run as a numeric non-root user, use an exec-form command, listen on an unprivileged port, and add an
image health check against `/healthz`. Pin the Python base to the 3.14 minor line and let the normal
dependency/image update process manage patch updates.

### Step 2: Add a smoke script

The script starts the image on an isolated local port, waits with a bounded retry loop, checks
`/healthz`, `/readyz`, and `/metrics`, verifies the container user is not root, and always cleans up
its own container. It must not stop or remove unrelated containers.

Run:

```bash
make docker-build-agent
make container-smoke-agent
```

## Task 5: Integrate The Developer And CI Interfaces

**Files:**

- Modify: `Makefile`
- Modify: `.github/workflows/continuous-integration.yaml`
- Modify: `docs/agents/README.md`
- Modify: `docs/agents/verification.md`
- Modify: `docs/development.md`
- Modify: `AGENTS.md`

### Step 1: Add Make targets

Add:

- `agent-sync` — locked development environment sync;
- `run-agent` — local Uvicorn development server;
- `verify-agent-format` — Ruff format check;
- `verify-agent-lint` — Ruff lint;
- `verify-agent-types` — strict Pyright;
- `verify-agent-architecture` — Import Linter and architecture test;
- `verify-agent-tests` — deterministic pytest suite;
- `verify-agent` — all previous checks in fast-failure order;
- `docker-build-agent` and `container-smoke-agent`.

Prefer these targets in docs and CI. Keep native parameterized commands in `agent/AGENTS.md` for
running one test or eval case.

### Step 2: Add an independent CI job

Add `agent-build-test` rather than coupling Python setup to the Java or frontend jobs. It must:

- install Python 3.14 and uv using a reviewed, pinned action/reference;
- restore an uv cache keyed by the lock file and Python version;
- run a frozen/locked sync;
- call `make verify-agent` from the repository root;
- require no provider or MCP secrets;
- upload useful test/coverage artifacts only if they remain free of prompt or secret data.

The container build may enter this job or remain a separate job depending on measured CI time. The
non-root container smoke is required before M1 is marked complete.

### Step 3: Update human and coding-agent guidance

- Add the third deployable and instruction-routing rule to root `AGENTS.md`.
- Put only Python-local commands and boundaries in `agent/AGENTS.md`.
- Add Agent rows to the verification decision matrix.
- Document local startup and environment-file handling without real keys.
- State that isolated docs changes do not require all three deployable gates.
- Keep stable command sequences in Make rather than copying them into several Markdown files.

## Task 6: Final Verification And Review

Run the narrow checks while implementing, then:

```bash
make agent-sync
make verify-agent
make docker-build-agent
make container-smoke-agent
git diff --check
git status --short
```

Review the dependency tree and built image contents for accidental provider SDKs, caches, test data,
or secrets. Confirm the service starts with no `.env` file and no external dependency.

Do not run Java, frontend, or Kubernetes verification for this isolated foundation unless their
files or contracts changed beyond the documented Make/CI integration. Report those skipped gates
and why.

## Commit Slices

Work directly on `master` in small cohesive commits:

1. `feat(agent): scaffold strict Python service`
2. `test(agent): add eval and architecture contracts`
3. `feat(agent): add health and telemetry baseline`
4. `build(agent): add non-root container`
5. `ci(agent): add verification workflow`

Before each commit, inspect the staged diff and run the narrowest relevant gate. Before declaring
M1 complete, run the full final verification above.
