# Movie Concierge Agent Guide

This guide applies to everything under `agent/`. Also follow the repository-root `AGENTS.md`.

## Working Directory

Run aggregate commands from the repository root. Run narrow uv commands from `agent/`.

## Architecture

- `imdb_agent.concierge` owns product policy, typed events, and orchestration Interfaces. It must not
  import FastAPI, Pydantic AI, web code, or concrete outbound Adapters.
- `imdb_agent.web` is the FastAPI/SSE inbound Adapter. It may use Concierge Interfaces but must not
  assemble outbound Adapters.
- `imdb_agent.adapters` contains concrete model, MCP, persistence, logging, and telemetry Adapters.
- `imdb_agent.bootstrap` is the composition root and the only place that assembles web and outbound
  Adapters.
- `imdb_agent.settings` validates environment configuration. Never pass the settings object through
  the product Module as a service locator.
- The Java backend owns Movie, Account, Engagement, Search Index, and Recommendation behavior. Use
  its protected MCP Interface; never access its PostgreSQL database or OpenSearch index directly.

## Python Rules

- Target Python 3.14 and manage dependencies with uv. Commit `uv.lock` and use locked syncs in CI.
- Keep Pyright in strict mode. Fix unknown or partially unknown types at the external Seam instead
  of adding blanket suppressions.
- Use the curated Ruff configuration. Do not enable every rule or add file-wide ignores without a
  documented reason.
- Use strict Pydantic models at environment, HTTP, MCP, provider, persistence, and event Seams.
- Keep provider-specific and Pydantic AI types inside their Adapters.
- Tests and evals must be deterministic by default and run without a provider key, Java process,
  database, search index, or network access.
- Keep synthetic eval cases under `evals/`; never copy production prompts or user data into fixtures.

## Telemetry And Secrets

- Never log or trace secrets, authorization headers, raw prompts, request/response bodies, or
  unrestricted tool inputs/results.
- Prometheus labels must remain bounded. Do not use account, conversation, movie, prompt, request,
  or trace identifiers as labels.
- Observability failures must not break the user request path.

## Commands

Narrow feedback:

```bash
uv run pytest tests/web/test_health.py
uv run pytest tests/concierge/test_eval_dataset.py
uv run ruff check src/imdb_agent/web tests/web
uv run pyright src/imdb_agent/web tests/web
```

Full gate from the repository root:

```bash
make verify-agent
```

Container verification:

```bash
make docker-build-agent
make container-smoke-agent
```

Before claiming completion, report the narrow checks, full gate, container check where relevant,
skipped cross-deployable checks, and remaining risks.
