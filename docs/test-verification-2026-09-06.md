# Complete test verification — 2026-09-06

Scope: the requested backend, frontend, browser E2E, Movie Concierge, supporting pipeline,
configuration and container checks. No production deployment was performed.

## Completed application checks

| Check | Result | Command |
| --- | --- | --- |
| Backend full build, formatting and unchanged coverage gate | Passed; 395 tests passed, one opt-in live-search test skipped in this invocation | `IMDB_CLONE_TEST_LLAMA_CPP=true ./gradlew build jacocoTestReport --rerun-tasks` |
| Backend fast behavior tests | 156 passed | Included in the full build |
| Backend semantic architecture tests | 27 passed | Included in the full build |
| Backend integration tests, including real local embedding | 212 passed, none skipped | Included in the full build |
| Live search relevance and latency | Passed; 12 query cases | `./gradlew liveSearchEvaluation --rerun-tasks` with `IMDB_CLONE_SEARCH_BASE_URL` pointing to the isolated test backend |
| Frontend behavior tests | 319 passed across 102 files | `cd frontend && yarn test` |
| Frontend lint, all TypeScript checks and production build | Passed | `cd frontend && yarn lint && yarn build` |
| Desktop/mobile Chromium E2E | 59 passed, one platform-specific skip | `PLAYWRIGHT_HTML_OPEN=never yarn e2e --workers=2 --reporter=list,html` from `frontend` |
| Movie Concierge | 86 tests passed; formatting, lint, strict types, import contracts and deterministic evals passed | `make verify-agent` |
| Movie seed pipeline | 26 passed, none skipped with Pillow available | `uv run --no-project --with pillow python -m unittest discover -s infrastructure/movie-seed -p 'test_*.py'` |
| Object-storage image pipeline | 3 passed | `python3 -m unittest discover -s infrastructure/object-storage/dev-seed -p 'test_*.py'` |
| Kubernetes manifests and release/runtime/Concierge/observability contracts | Passed; 42 resources schema-validated, 24 omitted for unavailable schemas | `make verify-kubernetes-schema` |
| Pinned observability charts and Alloy configuration | Passed; 35 resources schema-validated, three omitted for unavailable schemas | `make verify-observability-charts` |

Frontend, E2E, Concierge and supporting checks passed earlier in this same requested verification;
they were not repeated after the Docker cleanup because their source did not change. Browser tests
exercise the real UI with mocked API responses. Concierge tests/evals use deterministic providers;
they do not claim production model-provider integration.

The live-search run used separate PostgreSQL and OpenSearch containers with disposable in-memory
data and a separate backend process. The existing `light-local-trailers` image supplied 250 films
whose identifiers match the versioned judgement cases; the exact `light-2026-05-17` image tag was
unavailable. This is a run against that compatible local fixture, not proof that the historical
image was reproduced byte-for-byte. The local embeddinggemma service supplied real embeddings.
Results: MRR 1.000, nDCG@10 0.977, P@5 0.417, no zero-result queries and p95 latency 397 ms.
The test backend, temporary accounts, containers and test network were removed afterwards.

## Failure investigation and correction

An earlier broad run exhausted storage. Restarting Docker restored writes but left its separate
virtual disk almost full. A further run failed while starting the migration-test PostgreSQL
container; the container log explicitly reported no space left on device. The migration tests
passed separately, and the final complete backend build passed after cleanup.

With explicit user approval, unused Docker build cache and unused untagged images were pruned.
Docker reported 1.958 GB of build cache and 25.93 GB reclaimed from images. Existing containers,
tagged images and persistent data volumes were retained. Only disposable containers/volumes from
our interrupted tests were separately removed as test cleanup.

Enabling the optional embedding test exposed an independent configuration bug: its nested
`@TestConfiguration` caused application discovery to load the entire backend, then fail on missing
scheduler configuration before reaching the embedding service. The test now uses
`@SpringJUnitConfig` and explicit properties to load only `LlamaCppEmbeddingConfig` and
`MovieEmbeddingClient`. The existing real 768-dimensional, nonzero-vector assertions are unchanged.
This corrected test passes both alone and inside the final full backend build.

## Container verification

All three fresh linux/amd64 images built successfully and all three smoke commands exited with
code 0. Builds and smoke tests ran sequentially after the passing complete backend build.

| Deployable | Build and smoke commands | Result |
| --- | --- | --- |
| Backend | `make docker-build-backend DOCKER_IMG_BACKEND=imdb-clone-backend:test-20260906` and `make container-smoke-backend DOCKER_IMG_BACKEND=imdb-clone-backend:test-20260906` | Passed |
| Frontend | `make docker-build-frontend DOCKER_IMG_FRONTEND=imdb-clone-frontend:test-20260906` and `make container-smoke-frontend DOCKER_IMG_FRONTEND=imdb-clone-frontend:test-20260906` | Passed |
| Movie Concierge | `make docker-build-agent AGENT_IMAGE=imdb-clone-agent:test-20260906` and `make container-smoke-agent AGENT_IMAGE=imdb-clone-agent:test-20260906` | Passed |

The backend smoke verifies startup with a fresh PostgreSQL database, readiness/liveness and a non-root
UID. The frontend smoke verifies the SPA fallback and non-root UID. The Concierge smoke verifies
readiness, health, metrics and non-root UID with its deterministic fake provider. All run with a
read-only root filesystem, dropped Linux capabilities and no privilege escalation. Each script
removes its own disposable test containers; the backend script also removes its test database and
network. The frontend image build regenerated the API client inside the image and passed TypeScript
checks and the production build.

At completion, Docker's virtual disk has approximately 24 GiB free. Existing development services
are still running. `git diff --check` passes. Apart from this report, the verification changed only
the embedding integration-test configuration described above; no production code was changed by
this verification. All previously blocked checks in this verification scope are now complete.
