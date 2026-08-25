# Production Operations

This runbook is the operator entry point for the home k3s cluster. Public application traffic uses
HTTPS ingress. Databases, search, object-storage administration, metrics APIs, logs, traces,
profiles, and Argo CD remain private and are reached through SSH-backed Kubernetes port-forwards.

## Public URLs

| Surface | URL | Access |
| --- | --- | --- |
| IMDb Clone | `https://imdb-clone.the-coding-lab.com` | Public |
| Backend API | `https://backend.imdb-clone.the-coding-lab.com` | Public application API |
| Public movie media | `https://object-storage.imdb-clone.the-coding-lab.com` | Public objects only |
| Grafana | `https://grafana.imdb-clone.the-coding-lab.com` | Authenticated read-only viewer |
| Argo CD | `https://argocd.imdb-clone.the-coding-lab.com` | Operator IP allowlist |

Treat the Grafana viewer credentials as private. Loki contains operational metadata from every
namespace even though the application deliberately excludes prompts, responses, authorization
headers, secrets, and unrestricted tool payloads from logs and traces.

Copy the viewer password directly to the macOS clipboard without printing it:

```bash
make cluster-copy-grafana-viewer-password
```

## Private Operator Tunnels

From the repository root, start and inspect all tunnels:

```bash
make cluster-access-start
make cluster-access-status
```

Stop only the processes created by the access script:

```bash
make cluster-access-stop
```

The script defaults to `robotnik@um560`. Override `CLUSTER_SSH_TARGET` when DNS or the SSH user is
different. It stores only process IDs and non-secret SSH diagnostics below the macOS temporary
directory. It does not write credentials to disk or print them.

| Service | Local endpoint | Purpose |
| --- | --- | --- |
| PostgreSQL | `localhost:15432` | DBeaver or `psql` |
| OpenSearch | `http://localhost:19200` | Search API inspection |
| RustFS S3 API | `http://localhost:19000` | S3-compatible API |
| RustFS Console | `http://localhost:19001` | Object-storage administration |
| Grafana admin | `http://localhost:13000` | Full Grafana administration |
| Prometheus | `http://localhost:19090` | PromQL and HTTP API |
| Loki | `http://localhost:13100` | Log HTTP API through the Loki gateway |
| Tempo | `http://localhost:13200` | Trace HTTP API |
| Pyroscope | `http://localhost:14040` | Profile HTTP API |
| Argo CD | `https://localhost:18443` | Private Argo CD API and UI |

Use `make cluster-copy-grafana-admin-password` for the private Grafana administrator login. The
script copies the value to the macOS clipboard without printing or storing it.

These services remain `ClusterIP`; do not add public database, OpenSearch, Loki, Tempo, Pyroscope,
Prometheus, or RustFS-console ingresses.

## DBeaver PostgreSQL Connection

Use a normal PostgreSQL connection after starting the tunnels:

| Field | Value |
| --- | --- |
| Host | `localhost` |
| Port | `15432` |
| Database | `movie_db` |
| Username | `postgres_user` |
| SSL | Disabled; SSH already protects the transport |

Copy the application password directly to the macOS clipboard without displaying it:

```bash
make cluster-copy-postgres-password
```

Paste it into DBeaver and let DBeaver store it in the macOS Keychain. Use the application account
for routine inspection; do not use the PostgreSQL administrator account unless a runbook explicitly
requires it.

## RustFS Console

Start the tunnels, open `http://localhost:19001`, and copy the two credentials without printing
them:

```bash
make cluster-copy-rustfs-access-key
make cluster-copy-rustfs-secret-key
```

The console is enabled inside the cluster but has no ingress. Public traffic continues to reach
only the dedicated movie-media service on port `9000`.

## Logs, Metrics, Traces, And Profiles

Use Grafana Explore and its Metrics, Logs, Traces, and Profiles Drilldown views through
`http://localhost:13000` for operator work:

- Prometheus contains bounded application and cluster metrics.
- The frontend sends small same-origin batches containing Web Vitals, app/route timings, coarse API
  outcomes, bounded Concierge UI-action outcomes, and browser-error counts to the backend. The
  contract cannot carry URLs, messages,
  stack traces, user/session IDs, search text, or browser fingerprints.
- Loki contains pod logs from every namespace, Kubernetes Events, and the node's k3s systemd
  service logs with seven-day retention.
- Alloy normalizes JSON/plaintext levels, Nginx response classes, llama.cpp single-letter
  severities, and Kubernetes Event types into the shared `level` label used by Logs Drilldown.
- Traefik emits JSON operational and access logs without client addresses, request paths, query
  parameters, request lines, or headers.
- Tempo contains OpenTelemetry traces with three-day retention.
- Tempo TraceQL metrics and Traces Drilldown are backed by its local-blocks processor.
- Pyroscope contains continuously sampled Python CPU/allocation profiles and Java JFR
  CPU/allocation/lock profiles. Allocation profiles show allocation hot paths, not the amount of
  memory still retained; use Prometheus process/JVM memory metrics for current memory usage.
- A Tempo trace can open the CPU profile for the same service and time window. Python root spans
  additionally carry Pyroscope profile correlation IDs for span-level analysis.
- The `IMDB Clone – Operations Overview` dashboard is the operator landing page for availability,
  real-user experience, user-facing latency and errors, agent economics, cluster capacity,
  workload readiness, and actionable firing alerts.
- Dashboard navigation preserves the selected time range across Backend, Frontend, Movie
  Concierge, PostgreSQL, Infrastructure, Cluster Logs, Traces, and Profiles drill-downs.
- The `IMDB Clone Frontend` dashboard is the real-user view for LCP, INP, CLS, app/route timings,
  browser-observed API latency and outcomes, and anonymous browser errors. No signal can identify a
  browser or reconstruct the content of an error.
- The Backend dashboard includes catalog search, local embedding load, Hikari acquisition, GC,
  thread, and file-descriptor drill-downs. PostgreSQL adds session, transaction, lock, temporary
  data, and WAL views; Infrastructure adds throttling, load, network-drop, and inode views.
- The `IMDb Clone / Cluster Logs` dashboard is the log-focused starting point for workload
  failures.
- The `IMDb Clone / Movie Concierge` dashboard is the detailed view for agent cost, latency, tool,
  transport, and runtime metrics.

Grounded Concierge navigation exposes two complementary low-cardinality counters:
`imdb_agent_ui_actions_total{action="open_movie",outcome="emitted|rejected"}` records the server
policy decision, while
`imdb_frontend_ui_actions_total{action="open_movie",outcome="executed|rejected"}` records browser
handling. The corresponding trace events carry only the same action and outcome; movie IDs,
prompts, routes, accounts, and conversation identifiers are excluded.

llama.cpp exposes native embedding throughput and queue metrics through an internal ServiceMonitor.
OpenSearch and RustFS currently expose only Kubernetes workload readiness in the Operations
Overview. A ready StatefulSet or Deployment proves that Kubernetes considers the workload ready;
it does not prove native search or object-storage health. Service-native OpenSearch and RustFS
metrics, Argo CD reconciliation metrics, and Traefik request metrics require separate scrape or
exporter work and are deliberately not simulated by dashboard queries. Until that follow-up is
implemented, use the private OpenSearch and RustFS APIs, Argo CD UI, Traefik logs, and Kubernetes
workload state for deeper diagnosis.

If many unrelated pod streams contain the identical line `failed to create fsnotify watcher: too
many open files`, treat it as a node log-following capacity problem rather than an application
error. Alloy's Kubernetes log source can receive that line while opening container log streams and
store it under each affected target's labels. Ansible owns the persistent host setting in
`/etc/sysctl.d/99-k3s-inotify.conf`; verify the active value without changing it:

```bash
ssh robotnik@um560 'cat /proc/sys/fs/inotify/max_user_instances'
```

The expected value is `1024`. Rerun the Ansible playbook to reconcile drift instead of applying an
undocumented one-off `sysctl` change.

Agent logs and traces include safe correlation and operational fields only. They must never contain
raw prompts, model completions, tool arguments/results, authorization headers, API keys, account
IDs, conversation IDs, or movie IDs. Trace and request identifiers are structured fields rather
than Prometheus or Loki labels.

Useful read-only checks after starting the tunnels:

```bash
curl -fsS http://localhost:19090/-/ready
curl -fsS http://localhost:13100/ready
curl -fsS http://localhost:13200/ready
curl -fsS http://localhost:14040/ready
curl -fsS http://localhost:19200/_cluster/health
```

To inspect current cluster state without a tunnel:

```bash
ssh robotnik@um560 'kubectl get pods -A'
ssh robotnik@um560 'kubectl get applications -n argocd'
```

## Incident Workflow

1. Check Argo CD health and the affected Deployment rollout.
2. Open the relevant Grafana dashboard and establish when the symptom started.
3. Filter Loki by namespace, application, and the time window.
4. Follow a `trace_id` from a structured log entry into Tempo.
5. Inspect the FastAPI, Pydantic AI model/tool, and Java MCP spans without exposing content.
6. Open Profiles Drilldown for the affected service and time window; compare CPU, allocation, and
   Java lock hot paths with the trace latency.
7. Confirm Prometheus alert and resource trends before changing the workload.
8. Apply fixes through Git and Argo CD. Do not mutate a stateful production resource directly.

Alert rules are evaluated by Prometheus. Alertmanager notification delivery is still intentionally
disabled; choosing and securing an email, Slack, or another notification destination is a separate
operator decision.
