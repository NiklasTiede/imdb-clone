# IMDb Clone Movie Concierge

Python orchestration service for the Movie Concierge described in
[`docs/movie-concierge.md`](../docs/movie-concierge.md).

The foundation intentionally contains no provider SDK, model call, or Java MCP integration. It
establishes the strict development Interface, product/eval types, health endpoints, safe telemetry,
and container used by later milestones.

## Local Development

From the repository root:

```bash
make agent-sync
make run-agent
make verify-agent
```

The local server listens on `http://localhost:8090` by default:

```bash
curl --fail http://localhost:8090/healthz
curl --fail http://localhost:8090/readyz
curl --fail http://localhost:8090/metrics
```

No API key, Java process, database, or search index is required for this milestone.

## Package Map

```text
src/imdb_agent/
├── concierge/    product types, policies, and future orchestration Interfaces
├── web/          FastAPI inbound Adapter
├── adapters/     outbound logging and future MCP/model/persistence Adapters
├── bootstrap.py  composition root
└── settings.py   validated environment configuration
```

See [`AGENTS.md`](AGENTS.md) for Python-specific coding and verification guidance.
