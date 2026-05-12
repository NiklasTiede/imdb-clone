---
name: architecture-guardian
description: Use when reviewing this IMDB clone for architectural drift, module boundary erosion, persistence/schema mismatch, API/frontend contract mismatch, or readiness for Spring Modulith.
---

# Architecture Guardian

## Purpose

Review this repository for architectural drift and produce a read-only report. Focus on evidence, risk, and verifiable follow-up work. Do not edit source files, generated clients, migrations, config, tests, or commits while acting as guardian.

Build outputs from checks are allowed. Source changes are not.

## Repository Context

- Backend: Spring Boot, Java, JPA, Flyway, MySQL, Elasticsearch, MinIO, JWT.
- Frontend: React, TypeScript, Material UI, React Query, generated Axios client.
- Backend source: `src/main/java/com/thecodinglab/imdbclone`.
- Flyway migrations: `src/main/resources/db/migration`.
- Test data: `src/test/resources/sql/test-data.sql`.
- Frontend source: `frontend/src`.
- Generated frontend client: `frontend/src/client/movies/generator-output`.

Respect `AGENTS.md`. If repo metadata disagrees with implementation files, report it as project metadata drift instead of silently choosing one source.

## Modes

Choose the narrowest mode that satisfies the request:

| Mode | Use for | Load |
| --- | --- | --- |
| `quick` | Fast drift scan before or after a change | This file, then relevant references only if a concern appears |
| `persistence` | MySQL, Flyway, JPA entities, repositories, constraints, indexes | `references/persistence-jpa-mysql.md` |
| `backend-modulith` | Spring Modulith readiness or module boundary review | `references/spring-modulith.md` |
| `api-contract` | REST/OpenAPI/generated client/frontend API usage | `references/api-contract.md` |
| `frontend` | React feature architecture, shared modules, state/data ownership | `references/frontend-architecture.md` |
| `integration` | MySQL, Elasticsearch, MinIO, security, jobs, and source-of-truth flows | `references/integration-storage-search.md` |
| `full` | Broad architecture review across the whole system | All references, but summarize aggressively |

If the user does not specify a mode, default to `quick`. If they name a concern, choose the matching focused mode.

## Workflow

1. Identify the requested mode and state it.
2. Read only the reference files needed for that mode.
3. Inspect the repo before forming conclusions. Prefer `rg`, `find`, `sed`, Gradle/Yarn metadata, and existing tests.
4. Run narrow non-mutating checks when they add confidence. Examples:
   - `./gradlew test --tests "com.thecodinglab.imdbclone.integration.repository.DatabaseSchemaTest"`
   - `./gradlew test`
   - `cd frontend && yarn run lint`
   - `cd frontend && yarn build`
5. Produce a report using `references/report-format.md`.
6. Keep findings evidence-backed. Include file links, line numbers when available, risk, suggested fix, and verification.
7. Separate confirmed findings from hypotheses and readiness gaps.

## Subagents

Use subagents only when the user explicitly asks for parallel/delegated review or when the active environment permits it. For `full` mode, dispatch at most one read-only specialist per independent surface:

- Persistence specialist
- Backend Modulith specialist
- API contract specialist
- Frontend specialist
- Integration specialist

Specialists return findings only. The coordinator owns severity, deduplication, cross-system conclusions, and the final report.

## Severity

- `Critical`: production data loss, broken auth boundary, irreversible schema drift, or severe security exposure.
- `High`: real architectural drift that will cause bugs, migration failures, module erosion, or contract breakage.
- `Medium`: maintainability or testability risk with clear evidence.
- `Low`: cleanup, documentation drift, naming inconsistency, or early warning.
- `Readiness gap`: missing prerequisite for a planned architecture direction, especially Spring Modulith.

## Guardrails

- Do not change code while reviewing.
- Do not regenerate OpenAPI clients.
- Do not create migrations.
- Do not suggest broad rewrites when a focused seam, test, or rule would protect the architecture.
- Do not report generic best practices without evidence from this repo.
- Prefer deterministic follow-up checks when possible: ArchUnit, Spring Modulith verification, schema integration tests, OpenAPI generation checks, frontend lint/build/tests.
