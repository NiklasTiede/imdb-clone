# PostgreSQL retention

The Spring backend owns these policies. The Python Movie Concierge has no access to these
tables. Durations are operational defaults, not a claim about statutory retention requirements.

| Data | Default | Eligibility / configuration key |
| --- | --- | --- |
| Discovery telemetry (`discovery_event`) | 90 days after creation | `imdb-clone.recommendation.discovery.retention-days` |
| Security audit (`security_audit_event`) | 90 days after occurrence | `imdb-clone.identity.audit.retention-days` |
| Verification/reset tokens (`verification_token`) | 30 days after expiry | `imdb-clone.identity.tokens.retention-days` |
| Reindex history (`movie_search_reindex_job`) | 30 days after completion | Only COMPLETED/FAILED with `finished_at`; `imdb-clone.catalog.reindex.retention-days` |
| Mail deduplication metadata (`notification_delivery`) | 30 days after the later of completion and link expiry | Only SENT/EXPIRED with `completed_at`; `imdb-clone.notification.delivery.retention-days` |
| Login sessions | Existing session expiry | Spring Session owns cleanup; the configured inactivity timeout remains 14 days |

Cutoffs are exclusive: an entry exactly at the retention boundary is retained. PostgreSQL
transaction time determines eligibility, avoiding application-instance clock differences.
Discovery summaries operate over retained events; reducing its default below 90 days also
reduces the history available to the existing up-to-90-day summary endpoint. After reindex
history expires, its job ID is no longer queryable.

## Execution and operations

Each owning module has a transactional Spring scheduled cleanup. Runs start one minute after
backend startup and repeat 15 minutes after the previous invocation completes. Missed runs
need no separate ledger: the next run finds overdue rows directly in PostgreSQL.

- `imdb-clone.retention.interval=PT15M`
- `imdb-clone.retention.initial-delay=PT1M`
- `imdb-clone.retention.batch-size=1000` (valid range 1–10000)
- Each retention-days setting must be positive; invalid values fail startup.

Each invocation deletes at most one batch per table in a transaction with a 30-second timeout.
Time indexes support candidate selection. `FOR UPDATE SKIP LOCKED` skips active rows and lets
multiple instances safely share a backlog. Failed transactions roll back and the next scheduled
invocation retries. No entity collections are loaded and no unbounded delete loop runs.

Retention is an eligibility threshold, not a guaranteed deletion deadline: downtime, locks and
backlogs add delay. At defaults, one continuously running instance can remove approximately
96,000 overdue rows per table per day (less when executions take time). Successful nonempty
batches log only table and deleted count. Repeated full batches indicate a backlog; inspect the
oldest eligible timestamp and tune the interval/batch size if it keeps growing. A restart with
shorter retention starts applying the new threshold on the first scheduled run.

## Data deliberately preserved

- PENDING mail, RUNNING reindex jobs, and incomplete terminal records remain available for
  recovery/inspection regardless of age. Expired PENDING mail still goes through the delivery
  worker, which clears its ciphertext and marks it EXPIRED before retention can remove it.
- `movie_projection_work` and `media_object_work` are outstanding work, not history. Their
  existing workers remove records only after successful processing.
- `media_retired_token` remains permanent. Its tombstones prevent reattachment and repeat
  cleanup for arbitrarily late object-store writes; a time-based purge would weaken that contract.
- Accounts, passwords, passkeys, ratings, reviews and watchlists have no age-based deletion.
- Theme embeddings are a bounded derived cache keyed by theme ID, not an accumulating event log.
- The seed utility owns `movie_seed_run`; no cross-module backend cleanup is introduced for it.

Mail content is erased on send/expiry, independently of the additional 30-day metadata window.
The outbox refuses events whose original expiry is at or before database transaction time.
Consequently, replaying an expired event after its deduplication row was purged cannot recreate
pending mail. For links that are still valid, the completion row remains available for
deduplication. Event publishers must preserve the original token expiry on replay. SMTP remains
at least once; this does not provide atomic SMTP/PostgreSQL commits.

These policies affect live PostgreSQL rows. External logs/traces and existing backup copies have
their own storage lifecycles and are not purged by these jobs.

## Legacy OAuth tables

V16 drops `oauth2_authorization_consent` and `oauth2_authorization` with `IF EXISTS`, without
CASCADE and with a five-second lock timeout. The current backend is an OAuth2 client; it has no
authorization-server implementation or references to those tables. The local preflight found
four authorizations, all expired, and no consent rows. Their original creator remains unknown.

Fresh databases need neither table. Existing databases lose only these two legacy tables; an
unexpected dependent view/foreign key causes the migration to fail and roll back both drops.
This removes legacy authorization data, not Google/GitHub account links, passkeys or login
sessions. Production applies the migration only during a separately authorized deployment;
operators must first ensure no separately deployed authorization server uses those tables.

## Verification

`RetentionIntegrationTest` uses real PostgreSQL and production Spring transaction proxies. It
covers scheduler registration, exact cutoffs, bounded batches, concurrent row locks, rollback,
active-work preservation, mail replay after metadata removal, invalid configuration, clean
installs, legacy upgrades and atomic refusal of unexpected OAuth dependencies.

```bash
./gradlew integrationTest --tests '*RetentionIntegrationTest'
./gradlew build jacocoTestReport
```

Verified locally on 2026-09-09:

- `./gradlew spotlessApply integrationTest --tests '*RetentionIntegrationTest'`: nine passed.
- `./gradlew build jacocoTestReport`: successful, 403 passed, zero failures; the two opt-in
  live search/embedding checks were not enabled. Architecture, formatting and coverage gates passed.
- V15/V16 applied through the Flyway Java API against local `localhost:5432/movie_db`; all 16
  migrations validated. Both legacy OAuth tables are absent.
- One transactional batch of each production cleanup ran locally: two expired security audit
  records removed (54 to 52); all 888 discovery events retained within their retention window.
  Token, reindex and notification tables were already empty.
- No backend was listening on local port 8080 during maintenance. Periodic cleanup begins when
  the updated backend next starts. No production deployment was performed.
- Frontend/E2E and Python checks were not repeated: their code and contracts did not change.
