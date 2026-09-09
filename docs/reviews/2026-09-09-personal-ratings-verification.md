# Personal watchlist removal and rating actions — local verification

Date: 2026-09-09. Local development branch only; no production deployment or commit.
This extends the delegated watchlist slice documented in ADR 0004.

## Delivered behavior

- Explicit removal of one grounded film from the signed-in user's watchlist.
- Setting/updating a personal score from 0 to 10 with at most one decimal, and removal of that rating.
- English numeric and spoken scores; the final command binds operation, movie and score outside the model.
- Confirmed writes refresh and open `/your-watchlist` or `/your-ratings` in both voice and text.
- Actual changes offer Undo. Rating Undo restores the previous score, or removes a newly created rating.
- Unchanged operations produce an accurate notification without a misleading Undo button.
- Mobile notifications sit above the persistent voice controls.

Java owns the mutation, rating aggregate, login verification and atomic receipt. V18 preserves the
V17 receipts while extending the same ledger to `engagement_action_receipt`. One application-issued
operation ID is bound to its account, kind, movie and score. Domain state and receipt commit together;
replays cannot resurrect removed entries or delete later additions. Retention remains seven days.

Main implementation locations:

- `engagement/api/AssistantRatings.java`, `AssistantActionReceipt.java`, `AssistantWatchlist.java`.
- `engagement/internal/AssistantActionReceipts.java`, `AssistantRatingActions.java`, `AssistantWatchlists.java`.
- `assistant/internal/mcp/RatingMcpTools.java`, `PersonalToolAuthorization.java`, `PersonalActionResult.java`.
- `agent/src/imdb_agent/concierge/personal.py` and `adapters/personal_tools.py`; both runners share these.
- `frontend/src/features/concierge/components/ConciergeExperience.tsx` and `model/concierge.ts`.

## Automated verification

- `./gradlew spotlessApply build jacocoTestReport`: passed including coverage, 165 behavior tests
  discovered (1 opt-in skip), 27 architecture tests, 226 integration tests discovered (1 opt-in skip).
- `make verify-agent`: passed, Ruff, Pyright, import contracts, 201 tests and 27 deterministic eval cases.
- `yarn test --maxWorkers=2`: all 337 existing frontend tests passed; the subsequently added
  `personalActions.test.ts` also passed its 2 wire-contract tests.
- `yarn e2e concierge-voice.spec.ts concierge.spec.ts --reporter=line`: 14 passed, desktop and mobile.
  Covers refresh, navigation, previous-score Undo, removal Undo, unchanged state, voice continuity
  and accessible end-session controls while notifications are visible.
- `yarn lint` and `yarn build`: passed. Existing bundle-size/mixed-import warnings remain.
- `make docker-build-agent` and `make container-smoke-agent`: passed, Linux/amd64 image,
  readiness/liveness/metrics and non-root runtime checks.

The two backend opt-in skips are live search relevance and the local llama.cpp embedding endpoint.
Neither is required for these personal actions. An initial overloaded parallel frontend run hit
existing 5-second timeouts; limiting worker concurrency passed the entire suite without relaxing tests.

The protocol tests caught a nullable MCP output-schema mismatch. Live browser verification caught
Python's omission of null fields in voice events; the UI now normalizes absent score fields to null.
A dedicated regression test preserves legitimate zero scores and rejects invalid values/URLs.
Definitive policy rejection now uses PydanticAI `ToolFailed`, so a refused command does not exhaust
`ModelRetry` and terminate the voice connection. A provider-independent realtime test proves this.

## Live verification

Real local Java/PostgreSQL/MCP, Python/xAI and Chromium audio capture/playback are exercised using
macOS Samantha synthetic English WAV input and isolated, newly registered fixture accounts.
Credentials stay in process memory. No existing user library is changed or deleted.

The voice scenarios check each committed result through the ordinary authenticated REST API and
check the browser's actual destination. Watchlist add/remove/repeated removal passed with persisted
counts 1/0/0. In a subsequent uninterrupted voice connection, the complete rating sequence passed:

| Spoken command | Persisted result | Browser destination |
| --- | --- | --- |
| Please give Forrest Gump a rating of eight point five out of ten | One rating, 8.5 | `/your-ratings` |
| Change my rating for Forrest Gump to nine out of ten | One rating, 9.0; previous 8.5 in receipt | `/your-ratings` |
| Remove my rating for Forrest Gump | No rating; previous 9.0 in receipt | `/your-ratings` |
| Remove my rating for Forrest Gump (again) | No rating; unchanged receipt | `/your-ratings` |

All four commands used the same WebSocket without reconnecting. After logout, reusing its captured
in-memory delegation against Python returned HTTP 401. A further optional live text-channel test
stopped at test-account registration (HTTP 429), before reaching the agent. New rating mutations
were therefore verified live through voice, with shared text/voice policy and receipt mapping
covered by deterministic tests; this run does not claim a successful new live text scenario.
Synthetic inputs and temporary harnesses were kept under `/tmp`; no microphone recordings,
credentials or provider keys were added to the repository.

The short utterance “Rate Forrest Gump” was once transcribed as “Rainforest Gump”; the gate refused
it and the agent asked for a clearer command without modifying data or disconnecting. A slower
“Please give Forrest Gump a rating of eight point five out of ten” was correctly recognized.
Recognition quality remains provider-dependent; the application does not guess authorization.

## Remaining boundaries

General rating/taste reads, multi-step approvals and external enrichment remain separate roadmap
work. Existing voice budgets and the three-minute session limit remain. Undo is an explicit new
REST mutation, not a conditional restore against simultaneous edits in another browser tab.
A committed change is not rolled back by a later speech interruption or logout.
