# Version-Gated App Release Pipeline Design

## Context

The home cluster is deployed through Argo CD from
`infrastructure/clusters/home/apps`. Infrastructure manifest changes should be
deployed by normal GitOps sync after they land on `master`.

Backend, frontend, and Movie Concierge images should not be rebuilt and
deployed for every code-only pull request. They should move only when the
project app version changes.

## Version Source

Add one root-level `VERSION` file as the source of truth for the app release
version.

Backend, frontend, and Movie Concierge share that version because their
contracts and deployment should move as one coherent app release.

Seed images remain separate for now because they are large and data-driven.

## Release Trigger

The app release workflow runs on pushes to `master`, but publishes app images
only when `VERSION` changed in the pushed commit range.

Normal outcomes:

- Code-only pull request: CI runs, no app images are published.
- Infrastructure manifest pull request: CI runs; after merge, Argo CD applies
  the manifest change.
- `VERSION` bump: pull-request CI runs; after merge, backend/frontend images are
  built and pushed and a second pull request proposes the new k3s image digests.
  Argo CD deploys only after that deployment pull request is merged.

## Image Tags

For version `0.2.2`, publish:

- `niklastiede/imdb-clone-backend:v0.2.2`
- `niklastiede/imdb-clone-frontend:v0.2.2`
- `niklastiede/imdb-clone-agent:v0.2.2`

The workflow may also update `latest`, but GitOps manifests must pin the
version tag plus digest.

## Manifest Update

After pushing images, the workflow resolves image digests and updates:

- `infrastructure/clusters/home/apps/backend.yaml`
- `infrastructure/clusters/home/apps/frontend.yaml`
- `infrastructure/clusters/home/apps/agent.yaml`

The resulting image references use:

```text
repository:v<version>@sha256:<digest>
```

The workflow commits the manifest update to a deterministic
`release/v<VERSION>-deployment` branch and opens a pull request targeting
protected `master`. The deployment commit must run CI; it must not use a skip-CI
marker or a protected-branch bypass. Merging this pull request does not trigger
a second release build because it does not change `VERSION`.

## Verification

Before publishing images, the release workflow should require the same practical
checks as normal CI:

- Backend build/tests.
- Frontend dependency install and build.
- Movie Concierge checks and deterministic evals.
- Docker buildx setup and Docker Hub login.

E2E remains manual for now.

## Open Follow-Up

TLS automation and DNS cleanup are separate from app release automation.
