# Movie Concierge: voice, navigation and personal watchlist

**Status:** Step 0 English probe implemented and live-verified; application voice integration remains pending.

**Current example:** Forrest Gump (1994), using `forrest-gump-en.wav` and a fixed 142-minute
test record. This replaces the earlier fictional examples; the tool is still simulated, with no
live library lookup. Historical probe measurements below refer to their original fixtures.
**Date:** 2026-09-09
**Branch inspected:** `feature/agent-roadmap`, including merged backend architecture work.

This narrows the [long-term roadmap](../../movie-concierge-roadmap.md) around the requested
experience: talk through the microphone, discover movies, open a film, and add a film to the
authenticated user's watchlist while opening that list. Python remains the agent runtime.

**Language scope (user decision):** Build the first voice slice entirely in English: instructions,
spoken input/output, command interpretation and audio fixtures. Movies predominantly have English
catalog titles, which must be preserved in searches and replies. German dialogue is deferred;
future German commands still name movies by their English catalog titles. Localized title lookup
is a separate future capability.

## Verified baseline

| Area | Existing behavior | Missing for this slice |
| --- | --- | --- |
| Python agent | Pydantic AI 2.31.0, bounded text turns, typed SSE, MCP, fakes, evals and observability | Bidirectional audio session and xAI configuration |
| Domain tools | Search, details, similar movies and Tonight Mode via four Java MCP tools | Personal watchlist reads and writes |
| Navigation | Same-run grounded `open_movie`, validated by React | Watchlist/login destinations and contextual references in English |
| User identity | UI resets Concierge state when account identity changes | Independently verified delegated actor at Python/Java boundaries |
| Watchlist | Authenticated Java REST operation already exists at `PUT /api/v1/accounts/me/watchlist/{movieId}` | Narrow MCP capability, retry-safe action receipt and agent-driven UI refresh |
| State | Bounded process-local conversations and budgets | Defined voice disconnect/logout behavior; durable write idempotency |

The `X-Concierge-Client-ID` header is a browser-provided conversation identifier. Its account-ID
suffix is not authentication and must never authorize personal tools. The MCP bearer token proves
service identity only. Python must continue to access domain state through Java tools.

A direct policy probe confirmed that `Open Arrival` is recognized, but `Öffne Arrival`,
`Zeig mir den Film Arrival` and the local capability question `Was kannst du?` are not. This is
an English-only deterministic intent boundary; enabling German speech recognition alone cannot
fix it. This is a deferred multilingual limitation, not a prerequisite for the English voice slice.
The current action gate also requires exactly one grounded movie from the current run.

The installed Pydantic AI package contains `realtime/xai.py` and recognizes the requested model.
At the initial inspection, the optional `xai-sdk` dependency was not installed. Application settings
allow only OpenAI/fake backends, and the actual text runner constructs an OpenAI Responses model. Presence of the
library adapter is therefore not an operational voice integration.

## Proposed order

Do R1, the required parts of R2/R3, and a focused R7. TMDb enrichment, web research, a graph engine
and complete horizontal scaling are not prerequisites. Run the provider compatibility probe early
so its results can inform transport choices before the full interface is built.

### 0. Small provider and audio replay probe

- Pin `grok-voice-think-fast-2.0` and explicitly enable the Pydantic AI xAI realtime extra.
- Add provider-specific secret loading without changing the existing OpenAI-only secret file.
- Replay a synthetic English utterance through a local test driver; capture only approved fixture
  outputs, timing, usage and validated tool events.
- Exercise one fake tool, interruption, disconnect and configured spend/session limits.
- Verify pinned adapter compatibility with the live service; library presence is not evidence
  that all provider behaviors work. Record provider/transport choices in an ADR.

### 1. Useful application actions in text

- Extend the semantic action vocabulary with `open_watchlist` and `open_login`; keep URLs and
  route construction in React. No model-selected arbitrary navigation.
- Support English requests, negation and references to the displayed results, such as
  “Open the second one”. Resolve references against server-owned grounding, not an invented ID.
  Preserve English catalog titles; German command interpretation is deferred.
- Keep language interpretation separate from authorization. A structured interpretation is still
  untrusted until the requested action and catalog target have been checked.
- Define turn/action identifiers, duplicate suppression, cancellation and stale-event handling.
- Make capability help reflect the real channel, enabled tools and authenticated capabilities.
- Cover these behaviors with core evals, React tests and browser tests.

### 2. Delegated user and first watchlist action

- Design and document a short-lived session-bound delegated actor, issued through the existing
  authenticated application flow. Java independently checks audience, scope, expiry and session
  validity, including logout/account switch. Record the identity ADR and focused threat model.
- Derive the user from verified credentials outside model arguments. Never share one mutable
  MCP authorization context across concurrent users.
- Add `get_my_watchlist` and `add_movie_to_my_watchlist` through narrow Java module contracts.
- Treat an explicit, unambiguous add command as authorization for this low-risk action; show a
  receipt and an undo option. Ask for clarification when the target is ambiguous. This proposal
  refines the MVP's blanket approval wording and must be reflected in the product contract.
- Persist idempotency/receipt state in Java before retries can cross requests. The existing
  sequential find-before-insert behavior is not proof of concurrent retry safety.
- On success, refresh the frontend watchlist query and open the known watchlist page. On failure,
  report the failure without claiming the film was saved. Java owns the actual mutation.
- Test parallel duplicate requests, foreign-account requests, expired delegation, logout,
  ambiguous film versions, already-listed films and timeout after a committed write.

### 3. Realtime voice over the same capabilities

- Follow the [voice UI proposal](../../movie-concierge-voice-design.md): existing theme and drawer,
  blue user/gold agent signal, route-persistent compact dock and explicit start/mute/end controls.
  Its interactive preview is simulated and does not implement microphone or domain actions.

- Add a bidirectional voice-session port and provider adapter; the existing text-only
  `ConciergeRunner.stream(RunRequest)` is not an audio transport. Share tool authorization,
  grounding, action validation and receipts between channels.
- Initial transport proposal: browser audio over an application WebSocket to Python; Python
  owns the provider session and executes the protected Java tools. Measure relay latency before
  deciding whether a direct browser transport is worth the additional control-channel design.
- Keep microphone/session state above individual route pages and the chat drawer. Opening a film
  or the watchlist must not silently terminate the voice conversation.
- Provide explicit mic start/stop, visible listening/speaking status, playback cancellation,
  interruption, text fallback and permission-denied handling.
- Never authorize a write from partial speech-recognition hypotheses. Finalize the command and
  target first. Interruption stops pending speech/work; it does not undo a committed database
  change. Use the receipt and explicit undo for that case.
- Define per-session duration, inactivity, concurrent connection and audio-cost limits. Existing
  token-based text budgets alone do not cover a long-lived audio session.
- Keep raw audio/transcripts out of production telemetry. Limit initial operation to one Python
  replica with explicit session expiry on restart; durable write receipts remain required.

## Repeatable audio feedback loop

Start with approximately 20–30 synthetic English cases, then add 5–10 voluntary English recordings
from the user. Synthetic speech is useful for repeatability; it does not prove microphone quality,
accent handling, interruptions or real-room performance. No voice cloning is needed.

Each fixture needs a manifest containing language, utterance/transcript, authentication state,
known catalog IDs, prior cards, expected tools, expected final watchlist state, expected UI action
and prohibited effects. Use WAV masters and convert to the transport format in the driver.
Audio alone is insufficient for interruption tests: record timing and conversation events too.

Representative cases:

- “Suggest a science-fiction movie under two hours.”
- “Open Arrival.” / “Open the second one.”
- “Add Arrival to my watchlist and show it to me.”
- “Open Dune.” with both film versions available: clarify first.
- “Don't save it, just open it.”: no mutation.
- “Show my watchlist.” while anonymous: sign-in path, no personal-state invention.
- Repeated add command / replayed tool call: one logical add.
- Correction, interruption, silence, noise, mic denial, dropped connection and logout mid-session.

Three test layers:

1. Deterministic policy/tool/action tests, fake realtime protocol events and mock browser audio in
   CI. These do not measure a live model's speech understanding.
2. Opt-in replay of the fixed audio corpus against Grok with an explicit cost budget. Evaluate
   tool/argument correctness, task completion, first-audio/action latency and failure behavior;
   retain fixture-only artifacts under a documented test retention policy.
3. Real microphone runs on desktop/mobile with English speech, background noise and interruptions.
   Repeat failing examples as reviewed regression fixtures rather than tuning only happy paths.

Success means both an authorized domain outcome and the correct user-visible page. A fluent spoken
claim or a navigation event without the watchlist update does not pass. Set latency thresholds after
measuring the first vertical slice; do not substitute provider marketing benchmarks for app latency.

## Credentials and sources

The required xAI model ID is `grok-voice-think-fast-2.0`. The API supports realtime WebSocket
sessions and tool calls. An xAI API key is server-side only; ephemeral tokens are available for
direct browser connections if that transport is selected later.

Prepare a separate development key through the [xAI console](https://console.x.ai/). The development
local location is `.secrets/movie-concierge-voice.local.env` with `XAI_API_KEY`; `.secrets/` is
already ignored by Git. The separate voice loader is now implemented for the opt-in CLI.
Do not paste the key into a chat or add it to frontend variables. The OpenAI secret loader rejects additional fields,
so adding an xAI key to its existing file would break validation.

Primary references checked on 2026-09-09:

- [xAI speech-to-speech API](https://docs.x.ai/developers/model-capabilities/audio/speech-to-speech)
- [xAI ephemeral tokens](https://docs.x.ai/developers/model-capabilities/audio/ephemeral-tokens)
- [Pydantic AI xAI realtime adapter documentation](https://github.com/pydantic/pydantic-ai/blob/main/docs/realtime/xai.md)

## Baseline verification

These are the pre-implementation baseline checks, not the current implementation results.

- `make verify-agent`: passed, including 86 tests, strict typing, formatting, import contracts and
  the deterministic eval dataset.
- `cd frontend && yarn test src/features/concierge`: 13 tests passed across three files.
- No live provider calls, microphone runs, full browser E2E, deployment or production changes were
  performed for this assessment. Only this proposal and its roadmap link were added.

## Step 0 implementation record — 2026-09-09

- Added the xAI realtime dependency extra, separate literal secret loader, pinned model adapter,
  bounded audio replay CLI, two Make targets and a synthetic German WAV fixture. The probe uses
  one fictional film tool; it does not connect to Java or modify account data.
- Added deterministic tests underneath the real Pydantic AI realtime/tool loop: valid audio/tool
  exchange, wrong target/runtime, confirmed versus unconfirmed interruption, dropped connection,
  caller cancellation, timeout/output limits, sample-rate rejection and redacted authentication
  errors. Secret loading and explicit live opt-in have separate tests.
- `make verify-agent`: passed, **107 tests**, **27 deterministic eval cases**, strict typing,
  Ruff checks and both import contracts.
- Agent image build and non-root/read-only container smoke passed; final image verification is
  recorded in the task's completion report. No frontend/backend code or browser contract changed.
- The initial live probe stopped before opening a provider connection: the expected root
  `.secrets/movie-concierge-voice.local.env` file was absent. Its location was checked without
  reading or displaying another secret file. This was resolved after the user saved the file;
  the subsequent English live results are recorded below.
- No microphone/browser E2E or deployment performed. Next: implement contextual English application
  actions and delegated watchlist capabilities.
- [ADR 0003](../../adr/0003-movie-concierge-voice-probe.md) records the probe boundary and known
  provider constraints; [agent README](../../../agent/README.md) contains replay instructions.

### English-first adjustment

The user clarified the initial language scope after the first implementation. The German probe
recording was replaced with `star-voyage-en.wav` (synthetic Samantha voice). Instructions, fictional
movie title, reply assertions, tests, CLI help and documentation now use English. The previous
German probe remains historical implementation context, not a requirement for this release.

With the key file available, live xAI verification passed:

- Normal English replay: one correct fixture lookup, matching spoken title/runtime and audio;
  7.619 seconds total, first audio at 4.532 seconds, 5.5 seconds of generated audio.
- Cancellation replay: one correct fixture lookup and provider-confirmed interruption;
  4.146 seconds total, first audio at 2.697 seconds.
- An earlier cancellation attempt correctly reported failure because it interrupted a spoken
  acknowledgement before the tool ran. The scenario now waits for the fixture lookup before
  cancelling; it still requires an actual interrupted provider response to pass.
- Both measurements include setup and are single synthetic runs, not microphone latency benchmarks.
  Cost estimates were unavailable (`null`); these were billable live calls.
- The English change passes `make verify-agent` (107 tests and 27 eval cases), image build and
  container smoke. No browser microphone or application-navigation claim follows from these probes.
