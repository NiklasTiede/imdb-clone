# Legacy Deployment Definitions

This directory contains the project's former Docker Compose and Docker Swarm deployment assets.
They are retained for historical reference and are not the current production path.

## Current supported paths

Local development uses the repository root:

```bash
make docker-compose-dev-up
./gradlew bootRun
make run-agent
cd frontend && yarn start
```

Production uses k3s, Argo CD, immutable application image digests, and SOPS-encrypted secrets:

- [k3s and GitOps guide](../kubernetes/README.md)
- [production operations runbook](../../docs/operations.md)
- [home-cluster manifests](../clusters/home)
- [release workflow documentation](../../.github/workflows/README.md)

The production application consists of the React frontend, Spring Boot backend, Python Movie
Concierge, PostgreSQL, OpenSearch, RustFS, Traefik/cert-manager, and the Prometheus/Loki/Tempo/
Grafana/Alloy observability stack.

Do not use the Compose or Swarm files below for a new production release. In particular, they do
not represent the current agent service, GitOps security boundaries, observability setup, database
seeding lifecycle, or immutable release process.

## Historical content

- `development/`: superseded development Compose definition.
- `production/`: superseded home-server Compose deployment.
- `production/docker-swarm-deploy/`: abandoned Swarm experiment.

These files may be removed in a separate cleanup once their historical value is no longer needed.
