# AGENTS.md

## Project

Full-stack IMDB clone with a Spring Boot backend and React frontend.

- Backend: Java 25, Spring Boot 4, MySQL, Elasticsearch, MinIO, JWT auth.
- Frontend: React 19, TypeScript, Material UI 9, TanStack React Query, generated Axios client.
- Backend source: `src/main/java/com/thecodinglab/imdbclone`
- Frontend source: `frontend/src`

## Working Directory

Run commands from the repository root unless a command says otherwise.

## Feedback Loops

Backend:

- Start backend: `./gradlew bootRun`
- Run backend tests: `./gradlew test`
- Run one backend test: `./gradlew test --tests "com.thecodinglab.imdbclone.SomeTest"`
- Format backend code: `./gradlew spotlessApply`
- CI-equivalent backend check: `./gradlew build jacocoTestReport`

Frontend:

- Install dependencies: `cd frontend && yarn install`
- Start frontend: `cd frontend && yarn start`
- Build frontend: `cd frontend && yarn build`
- Lint frontend: `cd frontend && yarn run lint`
- Run frontend tests once: `cd frontend && yarn test`

Infrastructure:

- Start dev services explicitly: `make docker-compose-dev-up`
- Stop dev services: `make docker-compose-dev-down`

## API Client

The frontend API client is generated from the backend OpenAPI spec.

- If backend API contracts change, start the backend first.
- Update the spec: `cd frontend && yarn run updateOpenApiSpec`
- Regenerate the client: `cd frontend && yarn run build:moviesGen`
- Do not manually edit files in `frontend/src/client/movies/generator-output`.

## Backend Conventions

- Controllers expose REST endpoints and should keep business logic in services.
- Services use interfaces plus implementations under `service/impl`.
- Persistence uses JPA repositories and Elasticsearch repositories.
- API errors should use the existing `ProblemDetail`/global exception handling style.
- Integration tests use Testcontainers and shared fixtures from `src/test/resources/sql/test-data.sql`.

## Frontend Conventions

- Prefer existing Material UI patterns and components.
- Use the generated API client through `frontend/src/client/movies/MoviesApi.ts`.
- Keep server-state fetching and mutations in TanStack React Query wrappers under feature `api` folders.
- Keep feature-specific UI behavior inside `frontend/src/features/<feature>` and genuinely shared UI/hooks under `frontend/src/shared`.
- Keep reusable UI behavior in hooks or focused components.

## Database

- Flyway owns schema migrations in `src/main/resources/db/migration`.
- Local seed/test data uses SQL files in `src/main/resources/sql` and `src/test/resources/sql`.
- Do not modify existing database initialization or migration files unless explicitly asked.
- Do not introduce or rely on Liquibase unless the build and Spring config are wired for it.

## Agent Workflow

- Prefer small, focused commits.
- Run the narrowest relevant check first, then broader checks before reporting completion.
- Before committing Java, Kotlin, Gradle, or backend test changes, run `./gradlew spotlessApply`.
- Add or update tests for behavior changes.
- Do not commit secrets from local config files.
- Avoid unrelated refactors and formatting churn.
