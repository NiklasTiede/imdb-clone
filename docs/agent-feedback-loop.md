# Agent Feedback Loop

This document lists the commands agents and humans should use while reviving the project. Prefer the narrowest command that proves the current change, then run the broader checks before reporting a phase or feature as complete.

## Backend Checks

Run from the repository root.

| Task | Command |
| --- | --- |
| One backend test | `./gradlew test --tests "com.thecodinglab.imdbclone.integration.controller.MovieControllerTest"` |
| All backend tests | `./gradlew test` |
| Backend CI check | `./gradlew build jacocoTestReport` |

Notes:

- The backend test suite uses Testcontainers for MySQL, Elasticsearch, and MinIO.
- Gradle 9 requires `org.junit.platform:junit-platform-launcher` on the test runtime classpath.
- The Spring Boot Gradle plugin is intentionally newer than the runtime dependency BOM. The plugin is `4.0.5` so `bootJar` works on Gradle 9, while application dependencies are pinned to the Spring Boot `3.3.2` BOM until the planned runtime migration.
- The Gradle launcher may print Java native-access warnings when Gradle runs on Java 25. The application itself uses the Java 21 toolchain.

## Frontend Checks

Run from `frontend`.

| Task | Command |
| --- | --- |
| Install dependencies | `yarn install --frozen-lockfile` |
| Lint | `yarn run lint` |
| Tests once | `CI=true yarn test --watchAll=false` |
| Production build | `yarn build` |

Known current warnings:

- ESLint reports existing hook dependency and unused variable warnings.
- CRA reports stale Browserslist data.
- CRA may print `fork-ts-checker-webpack-plugin` TypeScript performance warnings on newer Node versions, while still compiling successfully.

## Local App Smoke Workflow

The local dev-services story will be simplified in Phase 1. Until then, use this workflow for a quick application smoke check.

| Step | Command |
| --- | --- |
| Start services | `docker compose up -d` |
| Start backend | `./gradlew bootRun` |
| Start frontend | `cd frontend && yarn start` |
| Stop services | `docker compose down` |

Expected local URLs:

- Backend: `http://localhost:8080`
- Actuator: `http://localhost:8081/actuator/health`
- OpenAPI: `http://localhost:8080/v3/api-docs.yaml`
- Frontend: `http://localhost:3000`
- MinIO API: `http://localhost:9000`
- MinIO console: `http://localhost:9090`
- Elasticsearch: `http://localhost:9200`

## Generated API Client

If backend API contracts change:

| Task | Command |
| --- | --- |
| Update OpenAPI spec | `cd frontend && yarn run updateOpenApiSpec` |
| Regenerate client | `cd frontend && yarn run build:moviesGen` |

Do not manually edit files in `frontend/src/client/movies/generator-output`.
