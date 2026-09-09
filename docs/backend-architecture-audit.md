# Backend architecture completion audit

Scope: the original seven-package architecture goal. All seven packages are implemented and verified
as of 2026-09-06. This audit maps requirements to reviewed implementation and executable evidence.
Decisions and operational limits are in [ADR 0002](adr/0002-backend-consistency-and-module-contracts.md).

| Requirement | Implementation reviewed | Executable evidence |
| --- | --- | --- |
| Token purpose, exact expiry, single use and concurrent consumption | Identity's `VerificationToken.consume`, locked consumption query, UTC clock and transactional `IdentityAccess`; Account creation transaction | `VerificationTokenTest`, `IdentityConsistencyIntegrationTest`, existing authentication/token-flow tests |
| Immediate rating consistency through create/update/delete/account deletion, including first insert | Engagement account advisory lock and lifecycle interface; Catalog atomic aggregate delta; metadata dynamic updates; V13 historical repair plus durable projection work | `RatingConsistencyIntegrationTest`, `RatingAggregateMigrationIntegrationTest`, rating/account controller tests |
| Media partial writes, rollback, orphan recovery and retry | Independent committed upload intent followed by REQUIRED write/attach transaction; Media token locks; owner image events; persistent cleanup and retirement audits; S3 adapter | `MediaServiceIntegrationTest`, `MediaRecoveryIntegrationTest`, `MediaConnectionPoolIntegrationTest` |
| Durable notifications, duplicate processing, failure and recovery | Encrypted transactional outbox, deduplication IDs, row-locked dispatcher, retry/expiry state and registered scheduler task | `NotificationConsistencyIntegrationTest`, `NotificationCipherTest`, `NotificationModuleIntegrationTest` |
| Durable incremental search and complete index rebuild | Desired revisions and current-state writer; durable reindex cursor; shared/exclusive reset coordination; retry/recovery tasks | `MovieProjectionConcurrencyIntegrationTest`, `MovieReindexDurabilityIntegrationTest`, projection atomicity tests and search controller tests |
| Explicit domain ownership and narrow module access | Account identity/media named interfaces; Engagement lifecycle/profile and Catalog ratings/reference/media interfaces; technical shared packages; module-local persistence/coordination and external adapters | Modulith graph verification, semantic dependency rules and isolated module bootstrap checks |
| Semantic enforcement, deliberate violations and module isolation | Shared ArchUnit rule objects over production bytecode and compiled invalid fixtures; STANDALONE PostgreSQL tests with foreign service/repository/entity exclusion | `architectureTest`, `BackendArchitectureRulesTest`, Engagement/Notification module integration tests |
| Documentation and mandatory verification | ADR 0002, implementation/evidence plan, verification matrix; `check` and CI require architecture, behavior, integration, formatting and existing branch-coverage gates | Full `build jacocoTestReport` plus inspected Gradle/CI dependency graph |
| Existing REST/MCP contracts and migration/data preservation | No request/response schema or route changes; additive V9–V14; previous Flyway migrations retained; source data preserved by repair | `OpenApiContractIntegrationTest`, `RestContractIntegrationTest`, MCP protocol/tool tests, `DatabaseSchemaTest`, upgrade rollback/retry tests |

## Final audit findings addressed

- Ordinary uploads originally nested journal registration inside a transaction holding a connection.
  They now use sequential scopes. Composed uploads preserve caller rollback and fail before remote
  writes when spare connection capacity is unavailable. Physical cleanup is scheduled after commit.
- Global Account API dependencies were narrowed to identity/media interfaces without changing REST DTOs.
- Scheduler restart tests now seed/reschedule recurring work before starting worker threads, avoiding
  a registration/execution race in the test itself.
- The full suite exposed idle connections retained by cached Spring test contexts. Test pools now
  release idle connections while retaining their maximum concurrency capacity.
- A concurrent second account deletion leaked `EntityNotFoundException` after waiting for the first
  deletion. A PostgreSQL regression reproduced it; the module now returns its normal not-found error
  and removes rating contributions only once.

## Verification record

The final `./gradlew spotlessApply` passed, followed by `./gradlew build jacocoTestReport`:
BUILD SUCCESSFUL in 2m 30s, exit code 0. Architecture, behavior and integration tasks all executed
in that full build; formatting checks and the unchanged branch-coverage gate passed.

Counts read from the completed `build/test-results/{test,architectureTest,integrationTest}/TEST-*.xml`:

| Task | Passed | Skipped | Failures/errors |
| --- | ---: | ---: | ---: |
| `test` | 156 | 1 | 0 |
| `architectureTest` | 27 | 0 | 0 |
| `integrationTest` | 211 | 1 | 0 |
| Total | 394 | 2 | 0 |

The two skips are existing opt-in external-service checks: live search evaluation
(`IMDB_CLONE_LIVE_SEARCH_EVALUATION=true`) and local llama.cpp embedding
(`IMDB_CLONE_TEST_LLAMA_CPP=true`). Neither test nor its enablement condition was changed.
All architecture-goal regression, concurrency, migration, recovery and isolated module tests ran.
The full run includes 12 Identity consistency, 11 rating consistency, 3 historical migration,
29 Media lifecycle/recovery/pool, 10 Notification consistency, 7 incremental projection concurrency
and 11 durable reindex cases. Fast tests include 8 token-invariant and 5 cipher cases.

Contract evidence includes 2 OpenAPI, 10 REST and 6 MCP protocol checks plus MCP tool tests.
The OpenAPI test compares the generated backend contract with the checked-in frontend YAML,
excluding only environment-specific server URLs. There is no contract drift, so no client
regeneration was required. The unchanged public REST controllers, assistant implementation and
frontend files were also checked against HEAD.

The complete tracked diff and new files were reviewed against the requirement table above.
Existing Flyway migrations V1–V8 are byte-for-byte unchanged; V9–V14 are additive. Source rating
preservation and migration rollback/retry are tested against isolated PostgreSQL schemas.
`git diff --check` passed. JaCoCo XML/HTML reports were generated under `build/reports/jacoco/test/`.
CI configuration requires `architectureTest` and the same full build; no remote CI run is claimed.
Frontend/agent/deployment checks were not run because those deployables and contracts were unchanged.
No deployment, production mutation or existing-data destructive operation was performed.

Targeted evidence and earlier failure/fix cycles remain in
[the implementation plan](backend-architecture-plan.md). After this full build only documentation
was finalized; no production or test code changed.

## Deliberate limits

SMTP delivery is at least once and can duplicate after remote acceptance before database commit.
Search rebuild is in place and can expose partial results while running. Media deletion is eventual;
retirement tombstones and periodic audits are retained, and arbitrary external/pre-journal orphan
objects are not inventoried. Composed uploads need spare journal-transaction connection capacity.
V13 rejects active old writers and must be retried after they finish. Notification encryption needs
a stable configured key before any future production rollout. Scheduler stop/restart is tested;
no OS-process crash or production deployment is claimed. None of these limits weakens token,
rating-transaction or durable-work persistence guarantees.
