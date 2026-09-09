# ADR 0002: Backend consistency and module contracts

**Status:** Accepted and implemented; verification recorded in
[the completion audit](../backend-architecture-audit.md) and
[the architecture plan](../backend-architecture-plan.md).

**Date:** 2026-09-06

## Context

Module dependency checks already enforce a closed Spring Modulith graph. They cannot prove that
business state survives concurrent requests, cascading deletes or partial failure. Regression
tests reproduced reusable/expired/wrong-purpose token consumption, partial Identity writes,
concurrent rating drift and account deletion leaving stale rating aggregates.

## Decision

### Ownership and Interfaces

- Identity owns registration/activation/reset policy and verification-token state. Account owns
  account profiles and credential persistence. Identity calls `account::identity`, which exposes
  identity lookup/mutation and registration availability contracts; it cannot use the broad Account
  API or repositories. The encompassing Identity use case owns the transaction.
- Engagement owns individual ratings and their complete lifecycle. Catalog owns the persisted
  movie aggregate fields and their Search Index projection. Engagement updates those fields only
  through `catalog::ratings`, keeping the existing immediate-consistency contract.
- Account calls `engagement::lifecycle` before deleting an account. That Interface removes rating
  contributions inside the same transaction. Database cascades remain a referential-integrity
  backstop and remove the remaining relations; they do not replace business orchestration.
- Media calls `account::media` and `catalog::media` for image references and consumes the respective
  public events. It cannot depend on the owners' broader APIs or persistence types.
- Technical coordination lives inside the owning Module. Shared does not become a workflow Module.
  REST and MCP remain Adapters to public module Interfaces. ADR 0001 remains unchanged.

### Token state and transaction guarantees

The token's consumption transition checks expected purpose, expiry (the exact expiry instant is
already invalid), and previous use. Legacy confirmed email tokens also count as used. A qualified
UTC Clock supplies time. Invalid transitions do not mutate token state or expose token material.

The consumption query takes a PostgreSQL row write lock. Account/credential mutation, token
consumption and credential audit writes share the outer use-case transaction. A competing request
waits for commit/rollback before validating the token state. Registration and administrative account
creation likewise encompass all related database writes. Remote I/O is not made atomic by these
transactions; durable mail handling is a separate work package.

### Rating concurrency and deletion

Every rating mutation first obtains a transaction-scoped PostgreSQL advisory lock for its account.
Account deletion obtains the same lock through the Engagement lifecycle Interface. This protects
the read/modify sequence even when the first rating row does not yet exist, and works across
application replicas without querying another Module's tables.

Under the application's PostgreSQL READ COMMITTED isolation, the lock is acquired before reading
the rating's previous state. The lock key is the server-side 64-bit hash of a namespaced account
identifier. An accidental hash
collision causes extra serialization, not incorrect behavior. Locks are automatically released at
commit or rollback. The lock Adapter requires an existing transaction.

Aggregate deltas remain atomic SQL updates owned by Catalog. Bulk account removal processes movie
IDs in ascending order to avoid opposing aggregate lock order. Source rating changes, aggregate
deltas, account deletion and persistent search tasks commit or roll back together. A stale
in-flight request after account removal cannot recreate an account: foreign keys reject it and its
transaction must roll back.

Movie metadata persistence uses Hibernate dynamic updates so that unchanged aggregate columns are
not written when another transaction changes ratings. This is only protection for independent
metadata fields; the rating read/modify sequence still requires its explicit lock. Concurrency
verification includes an image-token update overlapping a rating update.

### Media ownership and recovery

Account and Catalog own image references. Media owns the generated object variants and their
external lifecycle behind the internal `MediaObjects` Interface; `S3MediaObjects` is the storage
Adapter. Changing an owner's image reference emits a synchronous event containing the previous
and current token. The Media listener validates attachment and persists retirement in that same
transaction, including initial movie creation with an externally supplied token. Rollback therefore preserves the old
reference and its objects, including account/movie deletion rollback.

New uploads have a persisted lifecycle in `media_object_work` (additive migration V9):

| State/transition | Transaction and external effect |
| --- | --- |
| Register STAGED | Independent transaction commits before the first object write; eligible for abandoned-upload recovery after one hour |
| Write and attach | A REQUIRED TransactionTemplate starts after intent registration and locks STAGED throughout writes, reference update and intent removal; it joins an existing caller transaction |
| Successful attachment | New reference and removal of STAGED work commit together; previous token retirement commits in the same transaction |
| Failed or rolled-back attachment | STAGED work survives independently, including partial object writes |
| Recover abandoned STAGED | Skip active row locks, then mark RETIRED with a two-minute settlement interval |
| Retire old objects | Persist RETIRED work with the reference change; the scheduled worker cleans it after commit |
| Cleanup failure | Retain work, increment attempts and schedule another attempt after one minute |
| Cleanup success | After deleting both variants, retain a permanent V14 retirement tombstone and remove pending work; referenced tokens keep their objects |
| Retirement audit | After one day, queue idempotent cleanup again for retired tokens; repeat indefinitely to catch delayed remote writes |

Ordinary uploads validate/prepare outside a long database transaction, commit upload intent, and
then begin the write/attach transaction. These sequential scopes work with one application database
connection. Cleanup no longer opens a REQUIRES_NEW transaction in an after-commit callback while the
outer connection is still retained. Logical reference removal is immediate; physical deletion runs
asynchronously through the durable worker, so existing object URLs can remain usable until cleanup.

When an upload is composed inside an already active caller transaction, REQUIRED preserves that
caller's rollback semantics. Its independent intent registration necessarily requires another
connection. Pool exhaustion then fails before any object write, leaving references unchanged; a
one-connection PostgreSQL regression verifies this bounded failure. Such composed callers must size
the pool beyond concurrently retained outer transactions, as required by Spring's REQUIRES_NEW
contract. Flyway bootstrap may also need more than one connection independently of Media.

The existing db-scheduler infrastructure runs recovery once per minute. The batch query excludes
locked uploads so they cannot fill all 100 candidate slots. Each selected item is rechecked under
its own transaction lock. A fresh worker discovers pending work from PostgreSQL; memory is not the
source of recovery state. Task registration resolves the worker at execution time to avoid a
scheduler/bootstrap dependency cycle with Catalog.

Owner updates lock and refresh the current row before capturing the previous token. This makes
concurrent replacements retire the actual predecessor. An owner deleted during an upload causes
attachment to fail through the normal not-found contract, leaving recoverable upload intent.

Cleanup is at least once: a crash after remote deletion but before database commit repeats deletion.
The S3 Adapter treats deleting an absent key as success. Cleanup checks the owner's reference
Interface before removing objects. Token-scoped advisory transaction locks coordinate reference
changes with cleanup; workers use a nonblocking lock attempt and skip busy tokens. Reference events
lock old/new tokens in sorted order and reject tokens already pending retirement or recorded as
retired. Both create/update movie requests and profile-image changes use this guard. A failed guard
rolls back the owning reference update and its other database changes.

V14 retains `media_retired_token` tombstones after deletion. They cannot be pruned while guaranteeing
that retired tokens cannot be reattached and arbitrarily late storage writes will be caught. Recovery
queues up to 100 due tombstones per invocation, advances their next check by one day, and processes
them through the ordinary retryable cleanup protocol. Actual recheck latency also includes worker
availability and backlog; this is eventual cleanup, not a fixed-time deletion guarantee. The table
and periodic storage requests are an intentional cost of retaining that guarantee.

The two-minute settlement interval reduces unnecessary retries after upload ownership is lost;
the S3 client bounds complete calls to 45 seconds and individual attempts to 20 seconds. A client
timeout does not establish that the server stopped an earlier PUT. Persistent retirement audits
therefore remain necessary even after an apparently successful cleanup. Tests simulate repeated late
PUT completion after deletion and prove that subsequent audits remove the resurrected objects.
This is not a distributed transaction with S3.

`media.cleanup.pending`, `media.cleanup.completed{kind}`, `media.cleanup.failures{kind}` and
`media.retirement.audit_due` expose bounded metrics. Work rows retain failed-attempt counts and
retry timing. Normal cleanup retries indefinitely while work remains pending. No inventory of
objects created outside the lifecycle or before its journal/tombstones existed is claimed.

### Notification transaction and delivery guarantees

Identity publishes confirmation/reset requests synchronously inside the use-case transaction,
including the original token expiry. Notification persists the required delivery in its own
`notification_delivery` table (additive V10), joining that transaction with MANDATORY propagation.
There is no asynchronous in-memory listener between token issuance and persistence. Failure to
record the notification rolls back token issuance; rollback exposes no deliverable row to workers.

The existing db-scheduler infrastructure invokes `notification-delivery` every five seconds.
Each delivery obtains a row lock with SKIP LOCKED, checks eligibility/expiry and records its outcome
in a separate transaction. An SMTP/preparation/decryption failure retains PENDING work and retries
after one minute, or at expiry if earlier. After expiry the encrypted payload is erased and state
becomes EXPIRED, without sending. Successful sends become SENT and likewise erase encrypted content.
The original token lifetime is preserved rather than restarted at delivery.

A deterministic SHA-256 ID over notification kind and the high-entropy token link coalesces repeated
publication of the same request. SENT/EXPIRED IDs are retained until both completion and original
link expiry are older than the configured retention window (30 days by default). The outbox
rejects already expired events, so replay cannot reconstruct pending content after metadata
removal. See [PostgreSQL retention](../database-retention.md). Concurrent workers cannot send the
same locked row. Delivery is nevertheless
at least once: SMTP acceptance and the PostgreSQL commit cannot be atomic. A failure after SMTP
acceptance causes a retry with the same Message-ID; receivers may still show a duplicate. A real
PostgreSQL test rejects the completion update after the fake SMTP Adapter accepts the mail, then
verifies this replay. The registered scheduler is also exercised across stop/restart.

The outbox stores recipient, username and token-bearing link only inside authenticated ciphertext.
It uses Spring Security's existing `Encryptors.stronger` (AES-256-GCM, PBKDF2, random salt and IV),
with a required base64-encoded 32-byte secret. Each encrypted envelope binds its version and
delivery ID, so swapping payloads between rows is rejected. Named keys allow rotation while retaining
decryption of pending work. Successful/expired delivery removes the ciphertext and key reference.
See the [Spring Security crypto contract](https://docs.spring.io/spring-security/reference/7.0/features/integrations/cryptography.html)
and [configuration/rotation guidance](../development.md#environment-variables-and-secrets).

Notification owns queueing, encryption and delivery state. `EmailNotificationService` is the SMTP
and template Adapter; it does not listen to events or choose transaction semantics. The event and
payload diagnostic strings redact mail capabilities. Worker warnings omit exception details that
could contain recipients or links. Metrics expose pending count, sent, expired and failed attempts
with bounded kind labels; the table retains retry timing and attempt counts.

### Durable incremental Search Index projection

V11 adds Catalog-owned `movie_projection_work`, with one desired revision per movie. Creating,
updating or deleting a movie (including a rating aggregate delta) advances that revision inside the
domain transaction. A scheduler wake-up remains coalesced by movie ID but uses WHEN_EXISTS_DO_NOTHING.
It must not try to reschedule an executing task: db-scheduler rejects that operation, which previously
caused an otherwise valid concurrent domain change to roll back.

The worker reads the pending revision under a per-movie PostgreSQL advisory transaction lock,
then projects the current PostgreSQL state. The lock serializes queued/replayed projection writers
without locking the revision row across remote I/O. Domain mutations can therefore advance the
revision while an earlier index write is running. Completion removes only the revision that was
read. A newer revision survives and is found by the five-second recovery task after the previous
scheduler wake-up has completed. Database commit/rollback still governs both movie state and work.

Persisted UPSERT/DELETE operations remain compatible wake-up hints. Replayed work always reads the
current movie: an old DELETE hint cannot delete a movie that currently exists, and an old UPSERT
cannot resurrect a deleted movie. Legacy wake-ups without a V11 row use the same writer lock and
current-state projection; they do not create a row before remote I/O that would block a new change.
On failure they create retry work if no newer revision already exists.

External projection failures retain work, record failed attempts and schedule retry after one minute.
Failure to commit completion after an index write leaves work recoverable and may repeat the write.
`catalog.projection.pending`, `catalog.projection.failures` and `catalog.projection.completed` expose
bounded metrics; persistent rows retain retry timing and attempt counts. The original one-time task
descriptor and serialized payload remain supported across upgrade.

### Durable Search Index rebuild

V12 replaces in-memory reindex job state and the dedicated volatile executor with a Catalog-owned
job table and a recurring task in the existing scheduler. The accepted REST response commits only
a job; its identifier and progress can be queried after restart. A transactional advisory start lock
and a partial unique index permit one active job across instances. Existing response fields and routes
remain unchanged. Transient failures keep status RUNNING with a sanitized retry message; attempts
and the next retry time are durable. COMPLETED is written only after the finite scan finishes.

Reset takes an exclusive PostgreSQL advisory lock on the Search Index. Every incremental and rebuild
projection takes the shared form of that same lock before its per-movie lock. Reset cannot overtake
an in-flight writer. While holding the reset lock, the worker repairs the mapping when needed, clears
the index and captures the maximum movie ID. New inserts outside that finite range retain their
ordinary projection work. Domain transactions never take the index-writer lock.

The scan uses the next ID greater than the durable cursor, bounded by that captured maximum. Deleting
an already visited movie cannot shift an offset and skip another movie. Each film is processed through
the same current-state projection handler as incremental work. A separate transaction commits its
projection acknowledgement and cursor increment together. Failure after the remote write but before
commit repeats that film from current PostgreSQL state. Failure during reset leaves the RESET phase
recoverable; repeating the reset and scan is safe. The worker makes bounded batches of independently
committed steps, so a restart resumes rather than starting an untracked bulk operation.

Rebuild is an in-place operation: search can return incomplete results while the job runs. Completion
means the captured scan range has been processed; concurrent later mutations retain the ordinary
eventual projection guarantee. `totalMovies` is refreshed at reset and can differ from processed count
if movies are concurrently deleted. No zero-downtime alias swap or distributed atomic database/index
commit is claimed. Historical rating aggregates are corrected by V13 as described below.
Metrics expose `catalog.reindex.pending`, `catalog.reindex.failures` and `catalog.reindex.completed`;
job rows retain progress, retry timing and failed attempts without external exception messages.

### Historical rating baseline

V13 reconciles stored movie sum/count/average with Engagement's source ratings once during upgrade.
It updates only inconsistent derived columns, including clearing averages when no ratings remain.
The same transaction advances durable projection work for corrected movies, so rebuilding the search
index is not required merely to propagate those corrections. Source rating rows, metadata, correct
aggregates and unrelated pending work are preserved. The migration may access both modules' tables
as an explicit upgrade operation; runtime Catalog code still does not read Engagement persistence.

NOWAIT table locks on ratings, movies and projection work exclude concurrent old-version writers.
An active writer causes migration to fail without partial changes; retry after it finishes. This is
intentional fail-fast behavior rather than taking a potentially conflicting lock order while waiting.
Migration tests use independent PostgreSQL schemas at V12, introduce historical drift and verify the
upgrade, a failed enqueue with complete rollback, concurrent-writer exclusion and successful retry.
The application subsequently maintains these values through the existing synchronous delta contract.
Arbitrary external SQL mutations still bypass application ownership and require operator review.

### Evidence and enforcement

Test public module behavior, using persistence access only for fixture setup, controlled fault
injection and independent assertions. Real PostgreSQL tests hold one transaction open while a second
request runs. Verify final source rows, aggregate sum/count/average and task persistence; a successful
HTTP response or mocked method invocation alone is insufficient.

Retain Modulith's module-graph verification and closed named Interfaces. Replace the former Java
source scans with semantic ArchUnit rules over production bytecode and reflection on compiled
module/package annotations. The rules protect public contracts from implementation dependencies,
foreign internal access, persistence/S3/OpenSearch access from web Adapters, entities/repositories
outside internals, internal NamedInterface exports, business dependencies in shared, direct
Identity mail/Notification dependencies, volatile asynchronous event listeners and implementation
naming. Annotation composition and inherited listener methods are included.

The same rule objects evaluate deliberately invalid compiled fixtures, including fully qualified
references, generic return types and package-level exports. Positive fixtures ensure valid public
Interfaces and technical shared types remain allowed. Fixture packages live outside the application
root and are excluded from production imports, so they do not contaminate Modulith discovery.
These tests cannot prove the content of dynamic SQL strings or runtime reflection; the ownership
audit and real persistence/concurrency tests remain necessary.

Engagement and Notification also have STANDALONE `@ApplicationModuleTest` checks backed only by
PostgreSQL. Outgoing Catalog and SMTP Interfaces are mocked; foreign domain implementations and
repositories are explicitly asserted absent, along with checks of the exact JPA entity set.
Tests exercise rating persistence/rollback and committed notification
events through module contracts. Their non-web bootstrap deliberately avoids loading shared HTTP
transport configuration; full-context controller tests continue to verify REST behavior.

`architectureTest` is an infrastructure-free Gradle task required by `check` and `build`, and is
executed explicitly before the CI build. Ordinary `test` runs fast behavior tests; `integrationTest`
includes the isolated module checks. JaCoCo reports depend on all three tasks. See the
[verification matrix](../agents/verification.md) for complete and targeted commands.

## Consequences

- The existing synchronous rating response and REST/MCP request/response contracts remain intact.
- Per-account rating serialization intentionally favors clear correctness over maximum throughput
  for one account. Different accounts can proceed concurrently, subject to shared movie-row locks.
- The database is part of these consistency guarantees. Mocked or in-memory substitutes do not
  prove them. Arbitrary SQL scripts that mutate domain data bypass application orchestration.
- V13 establishes the historical rating baseline and queues corrections for Search Index recovery;
  subsequent arbitrary SQL writes still bypass the application protocol.
- The Media ledger covers writes performed through this lifecycle; it does not inventory orphan
  objects created before V9 or by external scripts. V14 guards tokens whose retirement is recorded
  by this lifecycle; it cannot reconstruct tokens deleted before any journal existed.
- Ordinary uploads use sequential connection scopes. Composed uploads retain their caller's rollback
  boundary and need spare capacity for independent intent registration; pool exhaustion is verified
  to fail before remote writes. Physical cleanup runs after commit through the persistent worker.
- Notification startup requires provisioning the new encryption key before deployment. No live
  secret or deployment is changed by this implementation. Notification tests use fake SMTP; they
  prove application delivery state and replay behavior, not acceptance by a production mail provider.
- The requirement-by-requirement audit and full backend build passed; the completion audit records
  executed checks and the two unchanged opt-in live-service test skips.

## Alternatives

Optimistic locking alone was not selected for ratings: first insertion and account deletion still
require coordination, and retries must encompass the entire business operation. Row-locking only
the rating also cannot lock a missing row. A durable per-account lock table would work but adds data
and migrations without improving the chosen transaction-scoped exclusion.

Asynchronous rating aggregates would change the existing immediate-consistency contract and add
duplicate/order handling. Revisit only if measured contention justifies that product tradeoff.

## Revisit triggers

Revisit if per-account write contention becomes material, independently deployed domain services
are required, or product requirements permit delayed rating aggregates.
