# Delegated watchlist: implementation and verification

2026-09-09. Local development slice; no deployment or commit performed for this task.

## Delivered behavior

Both text and English voice recognize a signed-in application session through Java-verified,
five-minute delegation. `get_my_watchlist` reads the actual user's collection. A complete explicit
add command invokes `add_movie_to_my_watchlist` only for an unambiguous catalog-grounded target.
React refreshes its watchlist queries and opens `/your-watchlist` after the committed receipt.
New additions show a receipt with Undo; existing entries are reported without a duplicate or Undo.
Anonymous watchlist navigation leads to the existing login page. Logout invalidates delegation.

Identity owns issuance and verification; Assistant maps MCP; Engagement owns the transaction and
receipt; Python owns command/grounding policy; React owns fixed routes and cache refresh. No Python
access to PostgreSQL or OpenSearch was added. See [ADR 0004](../adr/0004-concierge-session-delegation-and-watchlist.md).

## Changed areas

- Java `identity`: delegation contract/service and CSRF-protected issuance endpoint.
- Java `assistant`: application-only verified context, personal read/add MCP tools and registration.
- Java `engagement`: narrow assistant interface, shared REST/MCP mutation lock and durable receipts.
- Flyway V17: receipt table, account-deletion cascade and retention index; hourly seven-day cleanup.
- Python: session-owned MCP gate, verified conversation ownership, final-command policy, typed actions,
  authenticated text/voice transport and deterministic regressions.
- React Concierge: transient delegation via generated API, initial voice handshake, watchlist/login
  actions, query invalidation and receipt/Undo through the public Engagement feature interface.
- OpenAPI regenerated with the existing scripts; generated client output was not edited by hand.
- Product roadmap, local setup, implementation plan and ADR updated; TMDb/web remain deferred.

## Automated checks

| Command | Result |
| --- | --- |
| `./gradlew spotlessApply build jacocoTestReport` | Passed: 164 behavior, 27 architecture and 223 integration tests discovered; two pre-existing opt-in external-service tests skipped. Coverage gate passed. |
| `./gradlew integrationTest --tests '*EngagementModuleIntegrationTest'` | Passed, including six concurrent retries, one entry/receipt, already-listed handling, conflicting targets, replay after removal and rollback. |
| `make verify-agent` | Passed: Ruff, strict Pyright, import contracts, 164 tests and 27 deterministic eval cases. |
| `cd frontend && yarn run lint` | Passed. |
| `cd frontend && yarn test` | Passed: 337 tests in 105 files, including feature-boundary checks. |
| `cd frontend && yarn build` | Passed: app/tooling/e2e type checks and production bundle. |
| `cd frontend && yarn e2e concierge-voice.spec.ts concierge.spec.ts --reporter=line` | Passed: 12 desktop/mobile Chromium cases. Includes transient credential handshake, query refresh and persistent voice connection. |
| `cd frontend && yarn run updateOpenApiSpec && yarn run build:moviesGen` | Passed against the local updated backend. |
| `make docker-build-agent` / `make container-smoke-agent` | Passed: linux/amd64 image, non-root/read-only health/readiness/metrics smoke. |
| `git diff --check` | Passed. |

One existing audio-probe test hit its very short wall-clock deadline during overlapping heavy builds.
The final complete agent gate passed after those builds finished, without weakening the test.
The two backend skips are the opt-in live search-relevance evaluation and local llama.cpp embedding test.
The entire unrelated Playwright suite and Kubernetes deployment checks were not run for this slice.

## Real local browser/provider/backend checks

Dedicated generated local fixture accounts were used; the user's watchlist was not modified.
Credentials were kept in the test process, not in repository files. No real microphone was recorded.

Voice used synthetic macOS Samantha English speech injected into the browser's real microphone/audio
pipeline, the real xAI model, Python and Java MCP:

1. `Show my watchlist`: correctly reported an empty watchlist and opened its page.
2. `Add Forrest Gump to my watchlist and show it`: returned `created=true`; the page displayed Forrest Gump.
3. Same add command again: returned `created=false`, spoken already-listed acknowledgement, one stored entry.
4. All three commands used one voice WebSocket. No voice error events occurred.
5. After logout, reuse of the previous delegation through Python returned HTTP 401.

Text used the actual browser, OpenAI text runner and Java MCP:

1. `Add The Matrix to my watchlist and show it`: one stored entry and the correct watchlist route.
2. Clicking Undo removed that entry through the existing authenticated API.
3. Logout again invalidated the previously issued delegation (HTTP 401).

Temporary harness/evidence files are `/tmp/imdb-personal-live.mjs`, `/tmp/imdb-personal-live.log`,
`/tmp/imdb-personal-text-live.mjs` and `/tmp/imdb-personal-text-live.log`; they are not durable artifacts.
The assertions and outcomes above, plus checked-in regression tests, are the lasting record.

## Boundaries and next validation

The application remains English-first. Conditional, negated, ambiguous or incomplete add commands
fail closed; unsupported phrasing may need a repeated exact title/year command. Personal ratings,
agent-driven removal, TMDb and web search are not implemented. The UI's Undo is available for new
additions. Public voice rollout, noisy-room microphone behavior, comprehensive interruption/failure
recovery and latency targets remain separate work. An interruption cannot roll back a transaction
that has already committed. Synthetic successful turns are not a latency or real-microphone guarantee.
