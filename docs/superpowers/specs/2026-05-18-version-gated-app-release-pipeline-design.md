# Version-Gated App Release Pipeline Design

## Context

The home cluster is deployed through Argo CD from
`infrastructure/clusters/home/apps`. Infrastructure manifest changes should be
deployed by normal GitOps sync after they land on `master`.

Backend and frontend app images should not be rebuilt and deployed for every
code-only push. They should move only when the project app version changes.

## Version Source

Add one root-level `VERSION` file as the source of truth for the app release
version.

Backend and frontend share that version because the frontend depends on the
backend OpenAPI contract and the deployment should move as one coherent app
release.

Seed images remain separate for now because they are large and data-driven.

## Release Trigger

The app release workflow runs on pushes to `master`, but publishes app images
only when `VERSION` changed in the pushed commit range.

Normal outcomes:

- Code-only push: CI runs, no app images are published.
- Infrastructure manifest push: CI runs, Argo CD applies the manifest change.
- `VERSION` bump: CI runs, backend/frontend images are built and pushed, k3s
  manifests are updated to the new image digests, then Argo CD deploys.

## Image Tags

For version `0.2.2`, publish:

- `niklastiede/imdb-clone-backend:v0.2.2`
- `niklastiede/imdb-clone-frontend:v0.2.2`

The workflow may also update `latest`, but GitOps manifests must pin the
version tag plus digest.

## Manifest Update

After pushing images, the workflow resolves image digests and updates:

- `infrastructure/clusters/home/apps/backend.yaml`
- `infrastructure/clusters/home/apps/frontend.yaml`

The resulting image references use:

```text
repository:v<version>@sha256:<digest>
```

The workflow commits the manifest update back to `master`. The commit should be
marked so it does not recursively trigger a second release build.

## Verification

Before publishing images, the release workflow should require the same practical
checks as normal CI:

- Backend build/tests.
- Frontend dependency install and build.
- Docker buildx setup and Docker Hub login.

E2E remains manual for now.

## Open Follow-Up

TLS automation and DNS cleanup are separate from app release automation.
