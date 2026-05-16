# Integration, Storage, and Search Review

## Scope

Review cross-system behavior between MySQL, Elasticsearch, RustFS, JWT/security, scheduled jobs, and infrastructure scripts.

Primary files:

- `src/main/java/com/thecodinglab/imdbclone/*/internal`
- `src/main/java/com/thecodinglab/imdbclone/catalog/internal/persistence/MovieElasticSearchRepository.java`
- `src/main/java/com/thecodinglab/imdbclone/catalog/internal/persistence/MovieSearchDao.java`
- `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search`
- `src/main/java/com/thecodinglab/imdbclone/identity/internal/security`
- `src/main/java/com/thecodinglab/imdbclone/media/internal`
- `src/main/java/com/thecodinglab/imdbclone/engagement/internal/RatingAggregateScheduler.java`
- `src/main/java/com/thecodinglab/imdbclone/identity/internal/VerificationTokenCleanupScheduler.java`
- `infrastructure`
- `compose.yaml`

## Checks

### Source of Truth

- MySQL remains the source of truth for transactional data unless documented otherwise
- Elasticsearch documents are projections/search indexes with rebuild or repair paths
- RustFS stores binary objects addressed by stable tokens, not business state
- frontend image URLs derive from tokens consistently

### Consistency and Failure Modes

- write flows define what happens when MySQL succeeds but Elasticsearch or RustFS fails
- retries, exceptions, and logs preserve enough information to repair state
- delete flows clean up relation rows, search documents, and object references according to ownership rules
- scheduled jobs do not hide core consistency responsibilities

### Security and Access

- JWT and role checks align with controller semantics
- public endpoints do not expose private account data
- object storage access does not bypass application authorization for protected assets
- CORS and security config match frontend deployment assumptions

### Infrastructure

- local dev services match Spring configuration
- seed/import scripts match the current schema
- production and development compose files do not encode contradictory ports, bucket names, or index names
- monitoring/logging configs do not expose secrets

## Verification Recommendations

- integration tests with Testcontainers for cross-system write/delete behavior
- focused tests for RustFS object naming and image-token contracts
- search tests proving projection updates after catalog/rating changes
- security tests for public/private endpoint access
