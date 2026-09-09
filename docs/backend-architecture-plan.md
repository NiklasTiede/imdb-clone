# Backend architecture implementation plan

Started: 2026-09-06. Scope: the seven work packages in the active backend architecture goal.
This document records implementation and evidence, not permission to deploy.

## Baseline and constraints

The working tree was clean at the start. Reinspection confirmed missing token consumption
invariants, incomplete use-case transactions, rating lifecycle/concurrency gaps, pre-commit media
deletion, and non-durable asynchronous mail listeners. Current REST routes have evolved since the
initial review; preserve the current contracts. ADR 0001 remains authoritative for Java domain
ownership and the Python/MCP Seam. Preserve the nine-module arrangement and additive Flyway history.

## Work packages and completion evidence

| Package | Intended changes | Required evidence | Status |
| --- | --- | --- | --- |
| 1. Identity | Token state transitions, clock, serialized consumption, atomic registration/activation/password change and admin account creation | Purpose/expiry/reuse regressions; PostgreSQL concurrent consumption and rollback tests; existing authentication contracts | Complete; full build and token/transaction audit passed |
| 2. Engagement | Complete rating lifecycle, synchronous aggregate updates, serialization including first insert and account deletion | Concurrent insert/update/delete and account deletion tests; aggregate equals source ratings; projection task committed atomically | Complete; full build, concurrency and historical migration audit passed |
| 3. Media | Upload/attach/cleanup lifecycle; persisted cleanup and abandoned-upload recovery; storage Adapter | Partial upload, rollback, old-object retention, retry/replay and orphan cleanup tests | Complete; lifecycle, recovery, retirement and pool evidence passed in full build |
| 4. Notification/search | Durable follow-up work using existing scheduler where suitable; bounded observability; retry and projection recovery | Commit/rollback, failed delivery, repeat processing, restart/recovery and rebuild tests | Complete; durable notification, incremental projection and rebuild checks passed |
| 5. Module design | Narrow Interfaces and documented account/identity and engagement/catalog ownership; localize invariants; keep shared technical | Reviewed module contracts and semantic dependency checks | Complete; ownership audit and narrow-interface enforcement passed |
| 6. Enforcement | Semantic ArchUnit rules, negative fixtures, isolated module integration tests | Rules reject deliberate violations; isolated tests exercise public module behavior | Complete; semantic rules, negative fixtures and isolated-module checks passed |
| 7. Documentation/CI | ADRs, verification matrix and mandatory CI gates | CI configuration inspection; documented decisions match implementation; full build passes | Complete; documentation, mandatory CI configuration and full build audited |

## Implementation order

1. Reproduce Identity errors before changing production behavior, then implement and verify.
2. Apply the same regression-first loop to Engagement and Media in turn.
3. Complete reliable Notification/search processing and its failure tests.
4. Consolidate module contracts and add semantic enforcement and module isolation tests.
5. Record final ADRs and CI gates, review the entire diff, and audit every row above.

Prefer one coherent change per Module. Use real isolated PostgreSQL transactions for concurrency
and atomicity. Keep pure policy tests deterministic. Do not substitute mocked repository tests for
database guarantees. Record any prerequisite blocker while continuing independent work.

## Final gate (passed on 2026-09-06)

- `./gradlew spotlessApply`
- `./gradlew build jacocoTestReport`
- Relevant REST/OpenAPI and MCP contract checks, including drift checks if affected.
- Full diff and requirement-by-requirement completion audit.

## Evidence log

- 2026-09-06: repository and ADRs reinspected; Docker server available (29.7.2).
- Identity baseline: all 11 new consumption/rollback regression cases failed as expected; the
  separate admin-account creation rollback regression also failed before the production fix.
- Identity implementation: token entity owns purpose/expiry/single-use transitions; the repository
  locks the token for consumption; public mutating use cases own the database transaction. A
  qualified UTC clock supports deterministic time behavior. Legacy confirmed email tokens cannot
  be consumed again. No public request/response schema changed.
- Passed `./gradlew spotlessApply test --tests '*VerificationTokenTest' integrationTest
  --tests '*IdentityConsistencyIntegrationTest' --tests '*AuthenticationTokenFlowTest'`.
  Durable mail delivery remains work package 4; the current listener is not yet a completion proof.
- Engagement baseline: five of seven PostgreSQL cases failed before the fix (first insert,
  concurrent update, recreate after delete, account deletion, and deletion during an in-flight
  rating); the two existing rollback guarantees already passed and were retained.
- Engagement now serializes rating mutations and account deletion with an account-scoped
  transactional advisory lock, exposes `engagement::lifecycle`, and adjusts movie aggregates and
  persistent projection tasks before account deletion. Additional tests cover duplicate deletion
  and retaining another account's contribution.
- A further regression reproduced a concurrent movie-image update overwriting fresh rating
  aggregate columns. `Movie` now uses Hibernate dynamic updates for dirty metadata fields, while
  the rating aggregate continues to use its explicit atomic SQL update. This is not a replacement
  for serialization of rating operations.
- Passed `./gradlew spotlessApply test integrationTest --tests '*IdentityConsistencyIntegrationTest'
  --tests '*AuthenticationTokenFlowTest' --tests '*RatingConsistencyIntegrationTest'
  --tests '*RatingControllerTest' --tests '*AccountControllerTest'`: 171 fast tests reported
  (170 executed successfully, one skipped) and 41 selected integration tests passed.
- Added ADR 0002 for module ownership, token transitions and synchronous rating consistency.
  `git diff --check` passed. No application deployment or existing-data mutation was performed.
- Media baseline: both new rollback tests reproduced premature object deletion before the lifecycle
  change. Upload intent now commits independently before object writes; attachment and retirement
  share the owning transaction. An additive V9 work table survives rollback and supports retries.
- Eight PostgreSQL/RustFS recovery tests cover partial upload, rolled-back attachment, partially
  failed cleanup with metrics, replay, active-upload exclusion, referenced-object retention,
  simultaneous replacements, deletion during upload and recovery-batch fairness. A newly
  constructed worker reads existing database work; this is evidence of persistence independence,
  not yet a separate-process restart test.
- Two further tests failed before their fixes: 100 active uploads starved unrelated cleanup, and
  movie deletion during upload leaked a JPA exception instead of the module's not-found contract.
  Candidate selection now skips locked rows; owner refresh translates concurrent disappearance.
- Real scheduler bootstrap also exposed a registration dependency cycle. Recovery task registration
  now resolves the execution worker through ObjectProvider when the task runs. No circular-reference
  setting or test was weakened.
- Passed `./gradlew spotlessApply test integrationTest --tests '*MediaRecoveryIntegrationTest'
  --tests '*MediaServiceIntegrationTest' --tests '*RatingConsistencyIntegrationTest'
  --tests '*IdentityConsistencyIntegrationTest' --tests '*DatabaseSchemaTest'`: 171 fast tests
  (170 passed, one skipped), 52 integration tests passed. Follow-up `./gradlew spotlessApply` passed.
  ADR 0002 now describes the Media state transitions, consistency model, retry metrics and limits.
- Media now also has verified recovery through the registered recurring task across two actual
  Scheduler instances (stop/restart against the same PostgreSQL data), including a failed first
  scheduled cleanup. Account deletion during profile upload likewise rejects attachment and leaves
  recoverable objects. These are scheduler lifecycle tests, not OS-process termination tests.
- Notification baseline: a reset inside a rolled-back transaction still invoked SMTP. The regression
  failed with `Wanted at most 0 times but was 1` on the pre-change compiled production classes.
  The test-only mail health check is disabled because SMTP is mocked; production health configuration
  is retained.
- Notification now joins Identity's transaction through a mandatory synchronous outbox listener.
  V10 stores authenticated encrypted payloads, expiry, retry state and deduplication IDs. The existing
  scheduler handles dispatch; the SMTP/template Adapter no longer listens asynchronously to events.
- Ten PostgreSQL notification tests cover registration and reset delivery/consumption, rollback,
  enqueue failure, encrypted persistence, SMTP failure/retry, duplicate publication/completion,
  expiration, parallel workers, failure after SMTP acceptance and registered scheduler restart.
  Five fast cipher tests cover randomized encryption, payload/identity authentication, rotation,
  configuration validation and redacted diagnostics. No test contacts a real email recipient.
- Passed `./gradlew clean spotlessApply test integrationTest
  --tests '*NotificationConsistencyIntegrationTest' --tests '*MediaRecoveryIntegrationTest'
  --tests '*IdentityConsistencyIntegrationTest' --tests '*AuthenticationTokenFlowTest'
  --tests '*RatingConsistencyIntegrationTest' --tests '*DatabaseSchemaTest'`: 176 fast tests
  (175 passed, one skipped) and 56 integration tests passed. The clean rebuild resolved absent
  generated class files that had caused an earlier incremental compilation failure; sources and
  quality rules were retained. `git diff --check` passed.
- ADR 0002 and development guidance document Notification transaction/delivery guarantees, duplicate
  SMTP acceptance, expiration, bounded metrics, encryption-key provisioning and rotation. The
  production profile requires a new stable outbox key before any future deployment; no deployment
  or live-secret change was made.
- Search baseline: the real scheduler/PG/OpenSearch concurrency test reproduced
  `TaskInstanceCurrentlyExecutingException` when changing a movie while its old projection was
  writing. The scheduler client rejects rescheduling an executing task; the exception previously
  rolled back the domain mutation.
- V11 adds Catalog-owned desired projection revisions. Domain mutations advance a revision and
  coalesce a scheduler wake-up in the same transaction. Workers serialize by movie, read current
  PostgreSQL state and complete only their observed revision. Newer changes remain pending;
  five-second recovery finds them after the old wake-up completes. Failures retain retry timing and
  attempt counts. Existing serialized task descriptors remain compatible and legacy wake-ups do not
  lock a newly inserted ledger row across remote I/O.
- The initial expanded verification passed 176 fast tests (175 passed, one skipped) and 27 selected
  integration cases. These include movie update/deletion during a running projection, revision
  rollback, current-state replay, storage failure and scheduler restart.
- Passed the final `./gradlew spotlessApply integrationTest
  --tests '*MovieProjectionConcurrencyIntegrationTest'
  --tests '*MovieSearchProjectionTasksIntegrationTest' --tests '*RatingConsistencyIntegrationTest'
  --tests '*DatabaseSchemaTest'`: 28 integration cases passed. The seven projection scenarios cover
  both current and legacy wake-ups; after restart, the registered recovery task itself discovers
  failed work without a direct test call to the recovery method. The immediately preceding combined
  run also passed all 176 fast tests (one skipped). `git diff --check` passed.
- Existing bulk reindex still uses in-memory job status and writes stale page snapshots directly to
  the index. It has not been changed or claimed durable by the incremental-work implementation.
- Enforcement replaces source-text scans with compiled ArchUnit dependency/annotation checks and
  Spring Modulith verification. Nine shared semantic rules protect internal implementation access,
  public contracts, web/persistence separation, shared independence, Identity/Notification separation,
  persistence ownership, internal exports, durable listener dispatch and implementation naming.
  Eleven deliberately invalid compiled fixtures exercise those same rules, including fully-qualified
  references, generic signatures, package annotations and composed asynchronous annotations.
- Engagement and Notification now have standalone `@ApplicationModuleTest` coverage using only
  PostgreSQL and narrow outbound mocks. The tests exercise public rating behavior and public Identity
  events, transaction rollback and committed notification dispatch. They also verify that foreign
  domain services and repositories are absent. An initially suspicious global repository scan log
  did not reproduce a runtime isolation defect: Modulith removes foreign definitions before use.
  No production assembly change was needed.
- The separate `architectureTest` task requires no Docker. Both Gradle `check`/`build` and CI require
  it, and CI runs it before the complete backend build. `check --dry-run` confirmed the dependency
  graph includes architecture, behavior, integration, formatting and existing coverage verification.
  ADR 0002 and the verification matrix document these guarantees and their limits.
- Passed `./gradlew spotlessApply architectureTest test integrationTest
  --tests '*ModuleIntegrationTest'`: 27 architecture checks, 162 fast behavior tests and three
  PostgreSQL module tests passed; one existing fast test was skipped. The module tests additionally
  assert the exact JPA entity set (Engagement's three entities; none for Notification).
  This selected run is not the pending full-build or all-integration-test gate.

- Bulk reindex baseline: a new service instance could not find the job accepted by the previous
  instance; the PostgreSQL-backed regression failed with the existing not-found exception.
- V12 persists reindex jobs and their per-film cursor. A registered recurring scheduler task replaces
  the in-memory executor. Start requests serialize across instances. Reset excludes all projection
  writers through a shared/exclusive index lock; scan uses keyset traversal and the same current-state
  projection handler as incremental updates. Remote failures keep the job RUNNING with persistent retry
  state, and progress commits with each film's acknowledgement. The old snapshot/bulk-write path is
  removed. Search fixture preparation now exercises the production rebuild protocol.
- The initial combined run passed all architecture/fast checks and 31 selected integration tests,
  including nine rebuild scenarios, existing REST search contracts, seven incremental concurrency
  cases and ten schema checks. The earlier in-memory job unit scenarios now run against PostgreSQL;
  mapping adapter tests remain fast.
- Passed `./gradlew spotlessApply architectureTest test integrationTest
  --tests '*MovieReindexDurabilityIntegrationTest' --tests '*SearchControllerTest'
  --tests '*RecommendationControllerTest' --tests '*MovieProjectionConcurrencyIntegrationTest'
  --tests '*RatingConsistencyIntegrationTest' --tests '*DatabaseSchemaTest'`: 27 architecture checks,
  156 fast tests (one additional existing test skipped), and 49 integration tests passed. The eleven
  reindex cases include a PostgreSQL trigger rejecting progress after a successful index write and
  an exclusive reset waiting for an in-flight old writer while a domain update still commits.

- Follow-up `./gradlew spotlessApply integrationTest
  --tests '*MovieReindexDurabilityIntegrationTest'` passed all eleven rebuild cases after strengthening
  the restart test to resume after a successful first film and the reset failure to simulate loss of
  the response after the index was already cleared. Restart uses two actual Scheduler instances and
  the registered recurring task against the same PostgreSQL data; it is not an OS-process kill test.
  `git diff --check` passed. REST/MCP request/response schemas were not changed.

- Historical rating baseline: an isolated schema upgraded to V12 retained an incorrect sum of 100
  instead of the source ratings' 15. V13 now corrects only inconsistent sum/count/average values and
  advances projection revisions in the same migration transaction, preserving source ratings and
  movie metadata. Correct aggregates and existing unrelated/tombstone projection work remain intact.
- Passed `./gradlew spotlessApply architectureTest test integrationTest
  --tests '*RatingAggregateMigrationIntegrationTest' --tests '*RatingConsistencyIntegrationTest'
  --tests '*DatabaseSchemaTest'`. Upgrade tests also prove transactional rollback on enqueue failure,
  successful retry and refusing concurrent old-writer activity without partial changes. Migration
  uses NOWAIT table locks; a busy database requires retry after active writers finish.

- Media token-reuse baseline: two PostgreSQL/RustFS regressions failed because deleted tokens could
  be attached through movie update and creation. V14 now retains permanent retirement tombstones.
  Media validates old/current tokens synchronously under advisory locks, including movie creation
  and profile changes. Cleanup tries the same token lock without blocking active uploads, checks
  ownership, and records retirement atomically with completion of pending work.
- Due retirement tombstones periodically re-enter durable cleanup, so delayed remote PUT completion
  after an earlier successful delete remains recoverable. This intentionally retains metadata and
  incurs periodic storage requests; client timeout alone is not treated as a server-side fence.
- Passed `./gradlew spotlessApply architectureTest test integrationTest
  --tests '*MediaRecoveryIntegrationTest' --tests '*MediaServiceIntegrationTest'
  --tests '*RatingAggregateMigrationIntegrationTest' --tests '*DatabaseSchemaTest'`: 27 architecture
  checks, 156 fast tests (one skipped), and 40 integration tests passed. Fifteen Media recovery cases
  now include retired-token rejection on create/update, profile retirement, concurrent attachment
  versus cleanup, and repeated late object reappearance followed by periodic deletion.

- After adding the retirement-audit backlog gauge, `./gradlew spotlessApply architectureTest test`
  passed again. `git diff --check` is clean. The complete backend build and remaining pool/module
  audit have not yet been claimed complete.

- Pool baseline reproduced an ordinary upload requiring a second connection while holding the
  first. Upload preparation/journal registration now precede the REQUIRED write/attach transaction.
  Cleanup is exclusively scheduled from committed durable work; no after-commit callback requests
  another connection. The existing caller-rollback tests remain, and one-connection tests verify
  ordinary upload/delete plus failure before object writes when an enclosing transaction exhausts
  capacity. Flyway bootstrap capacity is separated from the application-pool experiment.
- Identity and Media now depend on `account::identity` and `account::media`, respectively, rather
  than the broad Account API. Existing Account credential/profile persistence, Identity policy and
  Engagement/Catalog aggregate ownership remain explicit. Semantic architecture checks enforce the
  narrowed declarations; no new runtime architecture framework or domain folder hierarchy was added.
- The focused pool/Media/module run passed. The first complete `build jacocoTestReport` then exposed
  exhaustion of PostgreSQL connections retained by cached test contexts. Test-only minimum idle
  connections are now zero, with idle eviction; maximum concurrency capacity is retained. The full
  gate will be repeated. An additional concurrent account-deletion error-contract regression is also
  being verified after the diff audit found a potential leaked JPA exception.

## Completion

The concurrent account-deletion regression reproduced the leaked JPA exception before its fix.
The corrected not-found behavior and single aggregate removal passed alongside the pool and
notification regression checks. The subsequent final `./gradlew spotlessApply` and
`./gradlew build jacocoTestReport` both passed (full build: 2m 30s, exit code 0).

The completed XML reports contain 394 passed tests: 156 fast behavior tests, 27 architecture tests
and 211 integration tests. Two existing opt-in live-service checks were skipped; no goal regression
was skipped. The unchanged coverage gate, formatting, OpenAPI/REST/MCP contracts and migration
checks passed. All seven packages were audited against their original requirements.

See [the completion audit](backend-architecture-audit.md) for the evidence mapping, exact skipped
checks, migration/API preservation and deliberate operating limits. Only documentation was finalized
after the passing full build. No implementation work remains within this goal; provisioning the new
Notification encryption key is a prerequisite of a future deployment, which was not requested.
