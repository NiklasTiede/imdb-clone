# Observability Review

## Scope

Review whether frontend, backend, infrastructure, and k3s observability surfaces
are coherent enough to debug production behavior without leaking secrets or
creating high-cardinality/noisy telemetry.

Primary files:

- `frontend/src/shared/observability`
- `frontend/src/app`
- `src/main/resources/config/*.properties`
- `src/main/java/com/thecodinglab/imdbclone/identity/internal/security`
- `src/main/java/com/thecodinglab/imdbclone/shared/logging`
- `src/main/resources/api-calls/Actuator.http`
- `src/main/resources/api-calls/llama-cpp/LlamaCpp.http`
- `infrastructure/clusters/home/apps/observability.yaml`
- `infrastructure/clusters/home/apps/observability/dashboards`
- `infrastructure/monitoring`
- `infrastructure/kubernetes/README.md`

## Checks

### Backend Metrics And Health

- Actuator exposes the intended endpoints only
- `/actuator/prometheus` is scrapeable by Prometheus without exposing private app data
- management port, service port, and ServiceMonitor endpoint names match
- health probes distinguish liveness from readiness where Kubernetes needs that distinction
- custom metrics, if added, avoid user identifiers, raw URLs with query strings, or unbounded labels

### Frontend Telemetry

- observability initializes once near app boot
- route metrics are mounted under the router
- URL/query sanitization prevents sensitive query params from entering telemetry
- browser errors and Web Vitals go through a small shared facade instead of feature-local ad hoc reporting
- feature components stay free of observability transport details unless a custom event is intentionally defined

### Logs

- backend logs go to stdout/stderr and preserve structured context for important IDs
- logs do not include JWTs, passwords, SOPS values, object-storage credentials, or personal data
- errors around search projection, embedding generation, storage, and scheduled tasks include enough context for repair

### Prometheus And Grafana

- Prometheus scrape contracts exist for backend and intended platform services
- ServiceMonitor selectors match the target services and namespaces
- Grafana dashboards are GitOps-managed when intended, and dashboard names are stable
- dashboards query real metric names exposed by the current app and chart versions
- public Grafana access is read-only and documented
- Prometheus/Grafana persistence, resource limits, and retention fit the home-cluster constraints

### Live Smoke Checks

- static CI should not depend on the home server
- optional manual smoke checks may query Argo CD health, pod readiness, Prometheus `up`, and Grafana datasource health
- external uptime checks cover public availability only, not internal scrape or dashboard quality

## Suggested Contract Tests

- backend `ActuatorSecurityTest` verifies anonymous Prometheus scrape access
- rendered manifests contain a backend `ServiceMonitor` with `/actuator/prometheus`
- rendered backend service exposes the management port expected by the ServiceMonitor
- dashboard ConfigMaps are included in the observability kustomization
- frontend tests verify URL sanitization, route metrics, browser error reporting, and app boot observability setup

## Report Guidance

Prefer debugging failure scenarios:

- "Prometheus cannot scrape backend metrics because the ServiceMonitor endpoint name no longer matches the service."
- "The frontend reporter would emit raw query strings."
- "A dashboard panel queries a metric that is not emitted by the current backend."
- "Grafana is public but configured with an admin-style account."

Do not require every technology to emit custom metrics. Require clear contracts for the
metrics and logs the project already depends on.
