# Infrastructure

The current production platform is a single-node k3s cluster reconciled through Argo CD. Local
development uses the root `compose.yaml`; older Docker Compose and Docker Swarm definitions remain
only as historical references and are not the production deployment path.

## Current platform

- React frontend, Spring Boot backend, and Python Movie Concierge application images.
- PostgreSQL as the domain source of truth and OpenSearch as a rebuildable search projection.
- RustFS as S3-compatible movie and account media storage.
- Traefik ingress and cert-manager HTTPS.
- Prometheus metrics, Loki logs, Tempo traces, Grafana dashboards, and Grafana Alloy collection.
- SOPS/age encrypted Kubernetes secrets and version-gated GitHub Actions releases.

Production manifests and cluster instructions live in
[`clusters/home`](clusters/home) and the [k3s README](kubernetes/README.md). Operator URLs, private
SSH tunnels, DBeaver access, credentials, and incident handling are centralized in the
[production operations runbook](../docs/operations.md).

## Directory map

| Path | Purpose | Status |
| --- | --- | --- |
| [`clusters/home`](clusters/home) | Argo CD applications and k3s resources | Production source of truth |
| [`kubernetes`](kubernetes/README.md) | Home-cluster bootstrap and release guidance | Current |
| [`movie-seed`](movie-seed/README.md) | Versioned PostgreSQL and RustFS data releases | Current |
| [`object-storage`](object-storage/README.md) | RustFS/S3 development assets and notes | Current |
| [`monitoring`](monitoring/README.md) | Observability stack overview | Current overview; legacy Compose assets retained |
| [`deployment`](deployment/README.md) | Historical Compose/Swarm deployments | Legacy only |

The PostgreSQL schema is owned by Flyway migrations under `src/main/resources/db/migration`. The
PlantUML data-model sources are [`imdb-clone-data-model.puml`](imdb-clone-data-model.puml) and
[`imdb-clone-physical-schema.puml`](imdb-clone-physical-schema.puml).

## Verification

Run from the repository root without applying anything to the cluster:

```bash
make verify-observability-production
make verify-observability-charts
make verify-kubernetes-schema
```

Deployments must flow through Git and Argo CD. Do not use ad hoc `kubectl apply` for releases.
