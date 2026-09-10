# ADR 0004: Session-bound Concierge delegation and watchlist actions

Status: Accepted for the local text/voice slice, 2026-09-09. Production rollout is separate.

## Decision

Java Identity issues a five-minute HMAC-authenticated capability through a CSRF-protected,
authenticated POST. Its claims bind the JDBC login session, account, expiry, fixed audience
`movie-concierge`, and `watchlist:read,watchlist:add,watchlist:remove,ratings:set,ratings:remove` scopes. A random signing key lives in that
session. Java verifies the persisted session and principal on each personal MCP invocation.
There is no second account database in Python and no new globally shared signing secret.

The browser holds the capability only for the current HTTP turn or voice connection. Voice sends
it in the first bounded WebSocket message, after Origin validation; never in a URL. Text sends it
in a dedicated header. Python verifies it with the workload-authenticated application-only
`get_my_context` tool before accepting personal conversation access. Conversation ownership combines
the existing browser identity with a Java-verified session binding, so a caller-chosen client ID
cannot retrieve another login session's conversation. Logout/account switches unmount the frontend
conversation and invalidate Java delegation. Text obtains a fresh capability per turn; voice ends
within three minutes, below its five-minute lifetime.

Python creates a separate MCP client/tool gate for each turn or voice session. Credentials and
operation IDs travel as application-injected MCP metadata, excluded from model argument schemas,
prompts and conversation history. Credentials never enter browser response events; operation IDs
appear in committed receipts so the UI can identify the action. Only Java MCP tools read or mutate
domain state. Anonymous model toolsets exclude personal tools and the application-only context tool.

## Write semantics

Only a complete affirmative English intention for one catalog-grounded film permits watchlist
addition/removal or personal rating set/removal. There is no prescribed command template:
“I want that one on my watchlist”, “Let's take this one off my list” and “I'd give it an eight”
are supported, including polite conversational prefixes. Setting includes updating an existing rating.
The final transcript must also contain the requested score (0–10, at most one decimal); numeric
and spoken English scores are supported. After a rating request whose only missing detail is the
score, the immediately following turn may supply just that score. This pending target is grounded,
cannot survive cancellation or an intervening turn, and cannot switch to another movie via tool
arguments. Text reconstructs it from the preceding user turn; voice retains it in session state.
The tool gate binds action, target and score together.
The model may not invent a personal score from IMDb data or a recommendation. Negation, conditions,
multiple targets, suggestions, missing/invalid scores and incomplete speech fail closed. Title
collisions require the year. A contextual `it`, `this one` or `that one` requires exactly one candidate.
An emitted movie-open action establishes that movie as the following turn's context; current-turn
catalog candidates are preserved until then, and new searches may introduce ambiguity again. Catalog strings
never grant permission. An interruption clears pending authority; a committed transaction remains
committed and is visible in the user's library.

Python injects one random operation ID per user turn. Engagement first serializes all assistant
mutations with a transaction-scoped account receipt lock, then takes the relevant existing domain
lock (watchlist or ratings), also used by REST mutations. The mutation and durable receipt commit
in one transaction. Ratings reuse the existing domain service and catalog aggregate updates.
Duplicate operation IDs replay their receipt; reuse for a different kind, movie or score is rejected.
A new operation for an already-satisfied state returns an unchanged receipt. Old additions cannot
resurrect subsequently removed entries; old removals cannot delete subsequently re-added entries.
V18 extends/renames the V17 ledger to `engagement_action_receipt`, preserving existing add receipts.
Receipts retain the previous personal score for Undo and expire after seven days; account deletion
cascades them. Credentials still live five minutes.

Typed `open_watchlist` / `open_ratings` events follow validated committed receipts. Explicitly
requested successful watchlist reads can also navigate. React refreshes the affected library and
movie queries, then navigates to the existing fixed route. The receipt notification distinguishes
changes from already-satisfied states and offers Undo only for an actual change. Undo uses existing
authenticated REST mutations to restore membership or the previous personal score (including no
rating). Definitive refusals use a failed tool result, allowing the model to ask for clarification without
exhausting retry limits and disconnecting. A model's spoken success claim alone never triggers navigation. Notifications sit above
the mobile voice controls so microphone and end-session buttons remain usable.

General personal rating/taste reads and broader account changes remain outside this slice.
Undo is an explicit new user action, not a compare-and-swap operation against edits in another tab.

## Focused threat analysis

- Forged account IDs: personal tools expose no account argument; Java resolves the signed session.
- Stolen workload key alone: it cannot access personal tools without a valid session delegation.
- Replayed expired/logout/account-switch credential: Java checks expiry and the current stored session.
- Cross-user Python state: no shared mutable credential client; conversation owners include verified binding.
- Prompt injection/partial transcripts: a pre-execution command/grounding gate bounds write authority.
- Ambiguous titles/model-selected IDs: retain all current-turn catalog candidates for write validation.
- Duplicate/time-out-after-commit requests: durable atomic receipts and database transaction locks.
- Browser CSRF/cross-origin socket: normal CSRF protection issues grants; voice validates allowed Origin.
- Credential leakage: no URL/storage/model/history placement; bounded handshake, redacted SecretStr,
  disabled model instrumentation for credential-bearing runs, safe operational logs.

A stolen delegation remains a bearer capability for its short lifetime and scopes. XSS defenses and
workload-secret protection remain necessary. Logout cannot roll back a transaction already authorized
and committed. This design does not claim guaranteed provider speech cancellation or durable voice
resume; those remain voice reliability work.

## Verification plan

Unit tests cover forged/expired/revoked/switched sessions and command/grounding gates. MCP protocol
tests prove metadata is not a model argument and workload-only requests cannot mutate. PostgreSQL
module tests cover concurrent duplicates, atomic failure, already-listed films and replay after
removal. Browser tests cover authenticated navigation/cache refresh, anonymous login navigation and
identity-change isolation. Run each affected deployable's complete gate and opt-in local smoke.
