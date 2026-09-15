# ADR 0004: Session-bound Concierge delegation and watchlist actions

Status: Accepted for the local text/voice slice, 2026-09-09. Production rollout is separate.

Updated 2026-09-11: The decision below supersedes the original transcript-grammar write gate,
three-minute voice limit, and exclusion of personal rating/recommendation reads. Casual requests,
title aliases and score-only follow-ups now rely on model-interpreted intent. Deterministic checks
still govern identity, grounded movie IDs, numeric scores, turn lifecycle and committed receipts.
This deliberately trades the old grammar restriction for conversational usability; it does not
claim deterministic protection against every misunderstood or injected natural-language intent.

## Decision

Java Identity issues a five-minute HMAC-authenticated capability through a CSRF-protected,
authenticated POST. Its claims bind the JDBC login session, account, expiry, fixed audience
`movie-concierge`, and `watchlist:read,watchlist:add,watchlist:remove,ratings:read,ratings:set,ratings:remove` scopes. A random signing key lives in that
session. Java verifies the persisted session and principal on each personal MCP invocation.
There is no second account database in Python and no new globally shared signing secret.

The browser holds the capability only for the current HTTP turn or voice connection. Voice sends
it in the first bounded WebSocket message, after Origin validation; never in a URL. Text sends it
in a dedicated header. Python verifies it with the workload-authenticated application-only
`get_my_context` tool before accepting personal conversation access. Conversation ownership combines
the existing browser identity with a Java-verified session binding, so a caller-chosen client ID
cannot retrieve another login session's conversation. Logout/account switches unmount the frontend
conversation and invalidate Java delegation. Text obtains a fresh capability per turn; voice has a five-minute wall-clock cap. Its delegated
capability also lasts five minutes, beginning before the voice connection is established, so Java
can reject a personal call shortly before the voice deadline. This does not extend or refresh the
credential. After 45 seconds without input, the session closes into browser standby; clicking the
closed Voice Lens starts a fresh session.

Python creates a separate MCP client/tool gate for each turn or voice session. Credentials and
operation IDs travel as application-injected MCP metadata, excluded from model argument schemas,
prompts and conversation history. Credentials never enter browser response events; operation IDs
appear in committed receipts so the UI can identify the action. Only Java MCP tools read or mutate
domain state. Anonymous model toolsets exclude personal tools and the application-only context tool.

## Write semantics

The model interprets the user's requested action, movie reference and personal score from the
conversation. Natural phrasing, title aliases (Amelie / Amélie) and score-only follow-ups do not
require a prescribed sentence template. Instructions require the model to ask about unclear movies
or scores, distinguish negation, hypotheticals and tool-result instructions from user commands, and
never invent a personal score from IMDb/community data. These are model behavior requirements,
not deterministic grammar guarantees. The previous final-transcript parser and pending-score
authorization state are superseded; final ASR text is retained for history rather than required as
an additional write permission.

Before execution, the tool gate checks an active uncancelled user turn, a delegated session, a
catalog-grounded movie ID, and one idempotent mutation per turn. Ratings must be finite numeric
values from 0 to 10 with at most one decimal; non-rating operations must not supply a score. A
repeated identical request can reuse the operation ID, while a different action, movie or score in
the same turn is rejected. All retrieved catalog candidates remain grounded; narrowing the visible
cards is not authority to discard candidates. An emitted movie-open action establishes that movie
as subsequent context. Interruption cancels the active turn and stale tool results cannot update
its state, but a Java transaction already committed remains committed.

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

Personal rating and taste reads are included: `get_my_ratings` returns paginated actual user scores
and the Java-owned taste summary, and `get_my_recommendations` derives suggestions from those
ratings using existing Java recommendation behavior. `ratings:read` authorizes these reads. Reading
ratings or recommendations does not itself request navigation or a mutation. Broader account
changes remain outside this slice.
Undo is an explicit new user action, not a compare-and-swap operation against edits in another tab.

## Focused threat analysis

- Forged account IDs: personal tools expose no account argument; Java resolves the signed session.
- Stolen workload key alone: it cannot access personal tools without a valid session delegation.
- Replayed expired/logout/account-switch credential: Java checks expiry and the current stored session.
- Cross-user Python state: no shared mutable credential client; conversation owners include verified binding.
- Natural-language intent: the model interprets requests under explicit instructions; the gate
  validates grounded arguments and lifecycle, not transcript grammar. Misinterpretation remains a
  model risk covered by deterministic scenarios and evals, with receipts and Undo for actual changes.
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

Unit tests cover forged/expired/revoked/switched sessions and grounding/score/lifecycle gates. MCP protocol
tests prove metadata is not a model argument and workload-only requests cannot mutate. PostgreSQL
module tests cover concurrent duplicates, atomic failure, already-listed films and replay after
removal. Browser tests cover authenticated navigation/cache refresh, anonymous login navigation and
identity-change isolation. Run each affected deployable's complete gate and opt-in local smoke.
