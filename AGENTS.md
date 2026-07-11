# IMDB Clone Agent Guide

Full-stack movie database app with a Spring Boot backend, React frontend, PostgreSQL,
OpenSearch, RustFS/S3-compatible object storage, and k3s GitOps deployment.

This is the auto-loaded fast-start contract. Keep detailed guidance in linked docs:

- `docs/agents/README.md` - agent workflow, ownership, task templates, review checklist.
- `docs/agents/verification.md` - verification matrix and feedback loop.
- `docs/left-shift-engineering.md` - compiler, type, test, and agent-feedback experiments.
- `docs/development.md` - local setup, env vars, smoke checks, troubleshooting.
- `docs/design.md` - frontend design system, theme tokens, layout primitives.
- `infrastructure/kubernetes/README.md` - k3s, Argo CD, SOPS/age, home-cluster notes.

## Working Directory

Run commands from the repository root unless a command says otherwise.

## Terminology

- Movie: primary catalog title.
- Person: actor, director, writer, or crew member; mostly future catalog work.
- CastMember: relation between Movie and Person with role or character metadata.
- Comment: current backend representation of a user-authored movie review.
- Rating: numeric score attached to a movie and account.
- Watchlist: saved or watched movie collection per account.
- Account/User: persisted account data and authenticated frontend/Spring Security principal.
- Media: posters, backdrops, profile photos, and files stored through RustFS/S3.
- Search Index: OpenSearch projection derived from PostgreSQL movie data.
- Deployment: k3s manifests, Argo CD root app, ingress, cert-manager, and SOPS secrets.

## Project Map

- `src/main/java/com/thecodinglab/imdbclone/` - backend modules.
- `src/main/resources/config/` - Spring configuration.
- `src/main/resources/db/migration/` - Flyway migrations.
- `src/test/java/com/thecodinglab/imdbclone/` - backend tests and Testcontainers support.
- `frontend/src/app/` - React providers and routes.
- `frontend/src/features/` - feature slices.
- `frontend/src/shared/` - shared API, auth, hooks, layout, media, and UI utilities.
- `frontend/src/client/imdb-clone-backend.yaml` - checked-in OpenAPI spec.
- `frontend/src/client/movies/generator-output/` - generated Axios client; do not edit manually.
- `frontend/e2e/` - Playwright tests.
- `compose.yaml` - local PostgreSQL, OpenSearch, RustFS, and seed services.
- `infrastructure/clusters/home/apps/` - k3s GitOps manifests rendered by Kustomize.
- `.codex/skills/` - Codex-specific reusable skills/tooling, not general project docs.
- `docs/agents/` - detailed agent workflow and verification guidance.
- `docs/assets/` - README/docs images.

## Ownership Rules

Backend:

- Public contracts live in `api`; web adapters in `web`; concrete services, repositories, mappers,
  and persistence types under each module's `internal` package.
- Modules are `account`, `catalog`, `engagement`, `identity`, `media`, `notification`,
  `recommendation`, and `shared`.
- Use existing `ProblemDetail` and `GlobalExceptionHandler` style for API errors.
- Flyway owns schema migrations. Do not edit existing migrations unless explicitly asked.

Frontend:

- Read `docs/design.md` before UI, layout, theme, or visual changes.
- Keep feature UI/behavior in `frontend/src/features/<feature>`.
- Keep genuinely shared API/auth/layout/media/hooks under `frontend/src/shared`.
- Use generated API clients through shared wrappers in `frontend/src/shared/api/`.
- Keep server-state fetches/mutations in TanStack Query wrappers under feature `api` folders.
- Prefer existing Material UI and theme patterns.

Infrastructure:

- Local services are owned by `compose.yaml` and Make targets.
- k3s app resources are owned by `infrastructure/clusters/home/apps`.
- App image versions are controlled by `VERSION` and the CD workflow.
- Kubernetes secrets are SOPS-encrypted `*.sops.yaml`; never commit decrypted values.

## Core Commands

```bash
make docker-compose-dev-up
./gradlew bootRun
cd frontend && yarn install && yarn run build:moviesGen && yarn start
make seed-local-users
make seed-light SEED_VERSION=2026-05-17
make reindex-local-search
make docker-compose-dev-down
```

Verification:

```bash
./gradlew test
./gradlew integrationTest
./gradlew build jacocoTestReport
cd frontend && yarn run lint
cd frontend && yarn test
cd frontend && yarn build
kubectl kustomize infrastructure/clusters/home/apps >/tmp/imdb-clone-home-apps.yaml
```

If backend API contracts change:

```bash
./gradlew bootRun
cd frontend && yarn run updateOpenApiSpec && yarn run build:moviesGen
```

## Safety Rules

- Do not expose secrets in prompts, logs, docs, screenshots, commits, or summaries.
- Do not print `.secrets/sops/age/keys.txt` or decrypted SOPS content.
- Do not commit `.env.local`, `.env.*.local`, private kubeconfigs, age keys, or Docker Hub tokens.
- Do not run destructive database, Docker volume, namespace, PVC, ingress, image tag, or cluster mutation commands without explicit user approval.
- Do not deploy to k3s unless explicitly asked. Read-only cluster checks are fine when relevant.
- Avoid unrelated refactors, formatting churn, and architecture rewrites.

## Definition Of Done

Before claiming completion, verify the relevant commands, protect secrets, respect ownership
boundaries, and report changed files, commands run, results, skipped checks, and remaining risk.
