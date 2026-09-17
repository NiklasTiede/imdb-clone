# Production Operations

The Popcorn Society domain switch was completed in September 2026. The
[migration record](popcorn-society-migration.md) documents the cutover, retained legacy routes and
rollback constraints.

This runbook is the operator entry point for the home k3s cluster. Public application traffic uses
HTTPS ingress. Databases, search, object-storage administration, metrics APIs, logs, traces,
profiles, and Argo CD remain private and are reached through SSH-backed Kubernetes port-forwards.

## Public URLs

| Surface | URL | Access |
| --- | --- | --- |
| Popcorn Society | `https://popcornsociety.app` | Canonical public application |
| Legacy public pages | `https://imdb-clone.the-coding-lab.com` | Permanent redirect to the canonical domain |
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
- The `Popcorn Society / Operations Overview` dashboard is the operator landing page for availability,
  real-user experience, user-facing latency and errors, agent economics, cluster capacity,
  workload readiness, and actionable firing alerts.
- Dashboard navigation preserves the selected time range across Backend, Frontend, Movie
  Concierge, PostgreSQL, Infrastructure, Cluster Logs, Traces, and Profiles drill-downs.
- The `Popcorn Society / Frontend` dashboard is the real-user view for LCP, INP, CLS, app/route timings,
  browser-observed API latency and outcomes, and anonymous browser errors. No signal can identify a
  browser or reconstruct the content of an error.
- The Backend dashboard includes catalog search, local embedding load, Hikari acquisition, GC,
  thread, and file-descriptor drill-downs. PostgreSQL adds session, transaction, lock, temporary
  data, and WAL views; Infrastructure adds throttling, load, network-drop, and inode views.
- The `Popcorn Society / Cluster Logs` dashboard is the log-focused starting point for workload
  failures.
- The `Popcorn Society / Movie Concierge` dashboard is the detailed view for agent cost, latency, tool,
  transport, and runtime metrics.

Grounded Concierge navigation exposes two complementary low-cardinality counters:
`popcorn_society_agent_ui_actions_total{action="open_movie",outcome="emitted|rejected"}` records the server
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

## Public voice rollout

The v1.5.0 backend also requires `NOTIFICATION_OUTBOX_KEY` in the SOPS-encrypted
`backend-runtime` Secret. It is a stable, base64-encoded 32-byte key for the email outbox,
explicitly required by the backend Deployment. Preserve it across releases and restarts;
replacing it without retaining the old named key makes pending deliveries unreadable.
See the notification outbox key-rotation notes in the [development guide](development.md)
before rotating it.

The public pilot allows **20 connected minutes per browser ID in any rolling 24 hours**, with
**100 minutes shared across all browsers and both models**, **two simultaneous sessions** and a
**ten-minute session lifetime** including connection setup. Restarts consume only actual connected
time, not a separate start allowance. The per-browser quota is independent of login and IP address.
Grok and GPT-Live remain selectable through the existing model menu. Inactivity and provider/tool
budgets can end a conversation earlier. This is a time budget, not an exact dollar cap.

Production credentials are projected from the SOPS-encrypted `movie-concierge-runtime` Secret:
`xai-api-key` and `openai-live-api-key` go only to the agent; `tmdb-read-access-token` goes only to
Java as `TMDB_READ_ACCESS_TOKEN`. The existing text key and MCP workload token are preserved.
Never put provider credentials into frontend builds, plain manifests or logs.

Release in this order to avoid enabling unsupported settings on the v1.4.0 image:

1. Merge the production support, encrypted secret projections and quota PVC with voice disabled.
2. Release v1.5.0 through the `VERSION` workflow and merge its generated image-digest PR.
3. Once all three deployments run the new images, add these environment values to `agent.yaml`
   in an infrastructure PR and let Argo CD reconcile it:

   ```yaml
   - name: POPCORN_SOCIETY_AGENT_VOICE_ENABLED
     value: "true"
   - name: POPCORN_SOCIETY_AGENT_VOICE_LIVE_ENABLED
     value: "true"
   - name: POPCORN_SOCIETY_AGENT_VOICE_SESSION_SECONDS
     value: "900"
   - name: POPCORN_SOCIETY_AGENT_VOICE_BROWSER_SECONDS
     value: "1500"
   - name: POPCORN_SOCIETY_AGENT_VOICE_SHARED_SECONDS
     value: "6000"
   - name: POPCORN_SOCIETY_AGENT_VOICE_QUOTA_DATABASE
     value: /var/lib/movie-concierge/voice-quota.db
   - name: POPCORN_SOCIETY_AGENT_VOICE_ALLOWED_ORIGINS
     value: '["https://popcornsociety.app","https://imdb-clone.the-coding-lab.com"]'
   ```

4. Verify `/concierge-api/v1/voice/models` returns both models. On the public HTTPS site, test
   microphone permission, playback, movie/trailer navigation and one signed-in personal action
   with each model. These consume connected time from the browser and shared allowances.
5. Check `voice_session_started`, `voice_session_ready` and `voice_session_ended` events for
   correlation and outcomes, without recording prompts or audio. Confirm every deployment is
   healthy and Argo reports `Synced`/`Healthy`.

`/var/lib/movie-concierge/voice-quota.db` stores only admission timestamps on the
`imdb-clone-voice-quota` PVC. Expired rows are removed at the next admission; no conversation or
account identifiers are persisted. A provider connection failure after admission also consumes a
start. Invalid start messages, unavailable models and rejected delegations do not. Database failures
fail closed. The rejection message tells users how many minutes remain until a slot returns.
Do not delete/reset the ledger to bypass the agreed budget. Keep one replica with `Recreate`;
concurrency accounting remains process-local, and the SQLite volume has one writer.

For rollback, disable both voice flags first. Preserve the PVC and remove the new voice settings
before rolling back to an image predating production voice support. Existing conversations end
when the pod is replaced; there is no automatic cross-pod session recovery.


Voice time quota rollout: publish and deploy new agent and frontend images together. Cached old
frontends must reload to send the required `browser_id`. The current GitOps manifest temporarily
keeps `POPCORN_SOCIETY_AGENT_VOICE_MAX_SESSIONS=8` for the pinned v1.5.0 image until that release is replaced;
the new agent ignores this legacy variable. Remove it after rollout. No quota database reset is
needed: `voice_usage` is created alongside the retained legacy `voice_starts` table. Time accounting
starts fresh because historical starts contain no durations. Never delete the PVC to reset limits.
