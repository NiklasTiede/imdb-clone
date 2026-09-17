# Diagram evidence

Reviewed 2026-09-17 against repository baseline `8fbf5e17`.
No production cluster access was needed for this documentation snapshot.

## Application

| Relationship | Repository evidence |
| --- | --- |
| Public HTTPS routes to static frontend, Java API and Python Concierge | `infrastructure/clusters/home/apps/components/popcorn-society/serve/ingress.yaml` |
| Nginx serves the React bundle | `frontend/nginx.conf`, `infrastructure/clusters/home/apps/frontend.yaml` |
| Java owns the domain and accesses the data services | `src/main/resources/config/application.properties`, `infrastructure/clusters/home/apps/backend.yaml` |
| Python calls protected MCP tools instead of accessing data stores | `agent/AGENTS.md`, `docs/adr/0001-movie-concierge-architecture.md`, `agent/README.md` |
| Voice is proxied through Python to the selected model provider | `agent/README.md`, `infrastructure/clusters/home/apps/agent.yaml` |
| Local embeddings, PostgreSQL, OpenSearch and RustFS | `infrastructure/clusters/home/apps/{llama-cpp,postgresql,opensearch,rustfs}.yaml` |

The map combines the browser with its React runtime and shows Nginx separately.
Traefik represents ingress/routing, not a claim of multiple application replicas.
Response paths are implicit. Public media delivery, OAuth providers, TMDB, certificate
issuance, seed jobs and detailed domain modules are deliberately omitted.

## Observability

| Relationship | Repository evidence |
| --- | --- |
| Java metrics are scraped through Actuator | `infrastructure/clusters/home/apps/observability.yaml` |
| Python and llama.cpp expose scrape targets | `infrastructure/clusters/home/apps/{agent,llama-cpp}.yaml` |
| Browser batches become bounded Java metrics | `docs/operations.md` (Logs, Metrics, Traces, And Profiles) |
| Alloy collects workload logs, Kubernetes events and k3s journal | `infrastructure/clusters/home/apps/alloy.yaml` |
| Java/Python send traces to Alloy, forwarded to Tempo | `infrastructure/clusters/home/apps/{backend,agent,alloy}.yaml` |
| Java/Python send profiles directly to Pyroscope | `infrastructure/clusters/home/apps/backend.yaml`, `agent/README.md`, `docs/operations.md` |
| Grafana queries four data sources and links logs/traces/profiles | `infrastructure/clusters/home/apps/observability.yaml` |

Java and Python share a source node for readability. Kubernetes workload logs include
their container logs and Traefik. Prometheus' cluster/exporter/PostgreSQL/llama.cpp
scrapes are explained in text, not individually routed in the diagram. Native
OpenSearch/RustFS metrics and public dashboards must not be inferred from this map.
