# Verification

This document is the compact verification matrix for agents and humans. Use
`../development.md` for full local setup and troubleshooting, and `README.md` for
agent workflow and review templates.

Prefer the narrowest command that proves the current change, then run the broader check before
reporting completion.

## Backend Checks

Run from the repository root.

| Task | Command |
| --- | --- |
| One fast backend test | `./gradlew test --tests "com.thecodinglab.imdbclone.SomeTest"` |
| All fast backend tests | `./gradlew test` |
| One backend integration test | `./gradlew integrationTest --tests "com.thecodinglab.imdbclone.SomeIntegrationTest"` |
| All backend integration tests | `./gradlew integrationTest` |
| All backend tests | `./gradlew test integrationTest` |
| Format backend code | `./gradlew spotlessApply` |
| Backend CI-equivalent check | `./gradlew build jacocoTestReport` |

Notes:

- `test` excludes the JUnit `integration` tag and does not start Testcontainers.
- `integrationTest` selects the `integration` tag and uses Testcontainers where PostgreSQL,
  OpenSearch, or RustFS are needed.
- Docker must be running for `integrationTest`, `check`, and `build`.
- `jacocoTestReport` combines coverage from the fast and integration test tasks.
- Before committing Java, Gradle, or backend test changes, run `./gradlew spotlessApply`.

## Frontend Checks

Run from `frontend`.

| Task | Command |
| --- | --- |
| Install dependencies | `yarn install --frozen-lockfile` |
| Lint | `yarn run lint` |
| Tests once | `yarn test` |
| Production build | `yarn build` |
| Theme token test | `yarn test src/theme.test.ts` |
| Playwright e2e | `yarn e2e` |

Notes:

- The frontend is a Vite app on `http://localhost:3000`.
- Vitest includes `frontend/src/**/*.{test,spec}.{ts,tsx}`.
- Playwright tests live in `frontend/e2e` and use desktop/mobile Chromium projects.

## Generated API Client

If backend API contracts change, start the backend first and then run:

| Task | Command |
| --- | --- |
| Update OpenAPI spec | `cd frontend && yarn run updateOpenApiSpec` |
| Regenerate client | `cd frontend && yarn run build:moviesGen` |
| Validate frontend build | `cd frontend && yarn build` |
| Check spec/client drift without rewriting tracked files | `make verify-openapi-drift` |

Do not manually edit files in `frontend/src/client/movies/generator-output`.

## Local App Smoke Workflow

| Step | Command |
| --- | --- |
| Start services | `make docker-compose-dev-up` |
| Start backend | `./gradlew bootRun` |
| Seed local users | `make seed-local-users` |
| Seed lightweight catalog | `make seed-light SEED_VERSION=2026-05-17` |
| Rebuild search index | `make reindex-local-search` |
| Start frontend | `cd frontend && yarn start` |
| Stop services | `make docker-compose-dev-down` |

Expected local URLs:

- Backend: `http://localhost:8080`
- Actuator: `http://localhost:8081/actuator/health`
- OpenAPI: `http://localhost:8080/v3/api-docs.yaml`
- Frontend: `http://localhost:3000`
- Object storage API: `http://localhost:9000`
- RustFS console: `http://localhost:9001`
- OpenSearch: `http://localhost:9200`

## Docker Checks

| Task | Command |
| --- | --- |
| Backend image | `docker build --platform linux/amd64 -t imdb-clone-backend .` |
| Frontend image | `cd frontend && docker build --platform linux/amd64 -t imdb-clone-frontend .` |

## Kubernetes Checks

| Task | Command |
| --- | --- |
| Render home app manifests | `kubectl kustomize infrastructure/clusters/home/apps >/tmp/imdb-clone-home-apps.yaml` |
| Render home app manifests via Make | `make verify-kubernetes-render` |
| Validate rendered manifests with pinned kubeconform | `make verify-kubernetes-schema` |
| Check Argo CD apps | `kubectl get applications -n argocd` |
| Check app namespace | `kubectl -n imdb-clone get deploy,svc,ingress` |
| Backend rollout status | `kubectl -n imdb-clone rollout status deploy/imdb-clone-backend` |
| Frontend rollout status | `kubectl -n imdb-clone rollout status deploy/imdb-clone-frontend` |

Do not run cluster mutation commands unless explicitly asked. The usual production app release path is
the version-gated CD workflow triggered by `VERSION` changes.

## Reporting Completion

Final responses should include:

- changed files,
- exact verification commands run,
- command results,
- commands skipped with reasons,
- remaining risk or follow-up work.
