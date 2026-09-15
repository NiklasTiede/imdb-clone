# Initial voice stress investigation — 2026-09-14

These are development diagnostics, not a provider ranking or a production readiness result.
See [the test guide](voice-stress-tests.md) for commands, assertions and limitations.

## Follow-up fixes and monitored run — 2026-09-15

The bounded stress command was repeated once for both models using report schema 2. These
results exposed local development restarts and therefore cannot rank provider reliability.
Artifacts: `frontend/test-results/voice-stress/2026-09-15T10-51-18-643Z/`, including a
payload-free `catalog-health.jsonl` recording of the Java ports sampled once per second.

- Grok completed 1 of 8 attempted tasks (10 scheduled). All 10 logged catalog tool calls succeeded.
  The Forrest Gump trailer action and route were correct, but the page heading remained absent.
  Other navigation requests and interruption corrections failed the fresh-action assertion.
  The session eventually reached a configured usage limit after 191 seconds.
- GPT-Live completed 5 of 7 attempted tasks (10 scheduled), including both attempted spoken
  interruption corrections. The first delegation failed with `tool_unavailable`; subsequent
  requests recovered. Successful task completion took 4.0–10.4 seconds. Rapid follow-ups still
  missed the requested final destination. On delegation 9 the existing 120,000-token cumulative
  backend budget was exceeded (131,616 recorded tokens, 20 model requests).
- Both Java ports disappeared together during 10:51:37–40 and 10:55:08–11 UTC, while the Java PID
  remained unchanged. Generated `META-INF/spring-configuration-metadata.json` was rewritten
  immediately before an observed restart. Spring DevTools monitors the classpath. The IDE is the
  suspected metadata writer; the process that wrote the file was not traced.
- Received and rendered recordings showed no clipping and no sustained tonal candidate above
  the review threshold (maximum rendered candidate 140 ms). This is not a listening assessment.

### Post-fix live validation (11:04–11:10 UTC)

Artifacts: `frontend/test-results/voice-stress/2026-09-15T11-04-26-567Z/`.
Grok attempted all 10 tasks and passed 5; it ended normally (`user_end`) with no input-backpressure
failure. Five of six ordinary requests passed, while both spoken-interruption and both rapid
follow-up cases missed the expected final action. Navigation rejection diagnostics included
`ungrounded_movie`, and two successful navigation tool calls could share one speech turn.
GPT-Live passed 5 of 7 attempted tasks, including four ordinary requests and one spoken correction.
It then reached `backend_tokens` (130,871 recorded tokens / 20 requests). Final usage was confirmed
(93 voice seconds), and the browser session correctly ended with **`voice_usage_limit`**, without
the former `input_backpressure` error. Existing limits were not increased.

Both reports initially flagged `catalogInterrupted`: their only down sample occurred **after**
`voice_session_ended`, during expensive audio evidence export. The Java listener identity was
unchanged. These flags are not evidence of an outage during the conversation. The monitor now
stops before evidence export and ignores in-flight probe results after stopping; original artifacts
remain unchanged, and their flagged aggregate group must not be used as a provider comparison.

The run also exposed an application restriction: Grok's relay and the shared browser hook accepted
only one navigation per speech turn. Server VAD can combine rapid requests into that single turn.
They now accept a different grounded movie/trailer destination in the same turn, while duplicate
navigation events and personal receipt destinations remain protected. Regression tests exercise
the public realtime relay, hook and real browser; missing catalog evidence and interrupted turns
remain rejected. Provider recognition of spoken interruptions is a separate remaining limitation.

### Focused rapid-command check (11:15–11:16 UTC)

`make test-voice-stress-live VOICE_STRESS_PROFILE=rapid` ran after the same-turn navigation
fix. Both runs reported `catalogInterrupted=false`, and both closed normally with final usage.
Artifacts: `frontend/test-results/voice-stress/2026-09-15T11-15-35-507Z/`.

- Grok passed the final Arrival destination check, 3,493 ms after the second fixture ended.
- GPT-Live failed the destination check. One delegated backend request completed in 2,506 ms
  with zero tool calls and zero UI actions. No transport, usage or provider error occurred.
  This isolates a remaining request-interpretation/delegation case; these logs do not identify
  whether transcript timing, captured request text, or model interpretation caused it.

No further paid retries were run. This focused case is not proof that the earlier long-session
spoken-interruption failures are resolved. The full stress suite remains red for those behaviors.

### Fixes and regression coverage

- Browser input queues yield briefly under bounded pressure rather than rejecting a buffered
  WebSocket burst before its consumer can run.
- GPT-Live continues draining microphone input while final usage is collected **and while the
  provider WebSocket closing handshake finishes**. Previously the latter gap let the input queue
  fill and replace the real usage-limit error with `input_backpressure`, despite confirmed final
  usage. The regression test exercises both delayed final usage and delayed WebSocket close.
- Delegated GPT-Live history is bounded to recent complete speech messages and the latest grounded
  result. Limits remain unchanged. The cumulative session budget can still end a long conversation.
- Transport failures abort a delegated request without blind retries of potentially committed
  writes. A later GPT-Live request uses a fresh MCP connection. Exceptions and budget diagnostics
  retain only allowlisted names/code locations; navigation tool results are now logged as well.
- Development config excludes only generated Spring configuration metadata from restart triggers.
  After `./gradlew processResources`, touching that generated file caused **no port interruption**
  during 16 seconds of 250 ms sampling. Runtime class/config changes still trigger normal reloads.
- Explicitly reopening the same failed movie route retries its query once. A failed retry does not
  loop. Both recovery and continued failure have component regression tests.
- Live tests now sample local catalog availability, record transitions, stop further tasks after
  an outage and identify the run as infrastructure-interrupted. Aggregates separate these runs
  from monitored healthy and older unmonitored runs.

## Follow-up after restarting the app (19:12–19:21 UTC)

All four outstanding live cases were executed once, without automatic retries:
`make test-voice-stress-live` and
`make test-voice-stress-live VOICE_STRESS_PROFILE=soak`.
Both commands failed their reliability assertions. Frontend PID 49817 and Java PID 49832
remained available at the recorded checks. Each scenario opened one voice socket and closed it;
the owned stress backend on port 8096 was gone after completion. The user's services were preserved.

| Model / profile | Attempted / scheduled | Raw passed assertions | Backend session duration | Ending |
| --- | ---: | ---: | ---: | --- |
| Grok / stress | 4 / 10 | 0 | 106.4 s | connection/provider error |
| GPT-Live / stress | 1 / 10 | 0 | 7.1 s | input backpressure |
| Grok / soak | 2 / 12 | 1 | 49.5 s | idle timeout |
| GPT-Live / soak | 10 / 12 | 4 | 210.9 s | input backpressure |

These are diagnostic assertion counts, not statistical provider success rates. In particular,
rapid follow-ups can observe an action from a preceding request; a page already at the expected
movie also returns immediately. Request-to-response attribution must be strengthened before
using these cases to compare completion latency or semantic task success. The failures below
are supported independently by backend logs. No test hit its configured hard time cap.

The user separately tested both models successfully during this investigation. Their original
Python service did not initially expose a model picker; this does not select the stress model,
since the harness starts its own process on 8096 with both providers enabled and the backend logs
confirm the selected model. Manual success is an important countercheck: these intermittent and
stress-specific observations do not establish that ordinary app conversations are generally broken.

### Concrete failures

1. **MCP connectivity and recovery:** Grok's first stress search failed with `ConnectError`
   (`default.py:118:map_httpcore_exceptions`), followed by 25 further failed tool attempts. Later
   errors came from `fastmcp/client/client.py:380:session`, which rejects an unconnected client.
   GPT-Live's first stress delegation failed during FastMCP connection setup at
   `client.py:622:_connect`; input backpressure ended the session afterward. Five separate MCP
   search probes and three probes after app assembly succeeded. The original connection failure
   is still unexplained; backend reachability at probe time does not prove every connection worked.
2. **Usage limits block later GPT-Live actions:** In the longer session, initial movie/trailer
   navigation and one spoken correction succeeded. Five later delegations ended with
   `error_code=usage_limit`, despite successful search calls, and emitted no UI action. This is a
   concrete reason for missing navigation in this run. Determine which request/token/tool budget
   is reached and reduce growing context before considering a limit increase.
3. **Audio input backpressure:** Both GPT-Live sessions ended with `voice_input_backpressure`.
   The long run's final browser error (`Cannot resume a closed AudioContext`) was the harness
   attempting another utterance after the session/audio context had closed; it is not evidence
   that this browser exception caused the session failure. The long run reached twelve completed
   delegation outcomes; investigate shutdown/delegation-limit handling alongside queue draining.
4. **Grok stops reacting:** The soak run opened Arrival in about 1.53 s after fixture completion.
   The next trailer request produced neither a tool event nor audible output, then the server
   ended the session for inactivity. Audio byte traffic continued, but the current report does
   not measure inbound speech energy, so recognition versus capture/transport loss is unresolved.

### Audio and evidence

All four received/rendered recordings had zero detected clipping. The longest rendered tonal
candidate was 80 ms, below the detector's sustained-tone review threshold. This does not exclude
stuttering or the user's intermittent beep; no subjective listening score was assigned.
Successful initial GPT-Live movie/trailer navigation took about 7.4–7.6 s; the spoken correction
check took about 16.5 s. These are task-completion timings, not first-spoken-answer latency.

Local, gitignored artifact roots (each case has `report.json`, `backend.json`, `received.wav`,
`rendered.wav`):

- `frontend/test-results/voice-stress/2026-09-14T19-12-42-398Z/` — stress.
- `frontend/test-results/voice-stress/2026-09-14T19-16-07-410Z/` — soak.

`make summarize-voice-stress` was run afterward. No application code, limits or model settings
were changed in this follow-up. Prior green deterministic gates were not repeated. No commits
or deployment were made. Next fixes should target MCP failure/recovery, the identified usage-limit
failures and input-queue/termination behavior before running another paid comparison.

## Earlier observations

- The deterministic browser suite passed for both model selections: early greeting during audio
  setup, twelve consecutive navigation/interruption cycles, duplicate actions, burst delivery and
  injected jitter. This validates the common browser contract, not provider barge-in intelligence.
- Two short Grok sessions produced one successful task out of six scheduled tasks. Tool failures
  included `ConnectError` followed by repeated `RuntimeError` failures. Direct catalog/MCP checks
  succeeded separately; this does not prove the backend remained available throughout every run.
- Three short GPT-Live sessions produced two successful tasks out of nine scheduled tasks; four
  tasks were never attempted because sessions ended. Two sessions showed a delegation runtime
  failure followed by input backpressure. The latter may mask an earlier failure. A subsequent
  session completed trailer navigation and a spoken interruption/correction. Its first movie-open
  action arrived, but the visible-page assertion failed; that discrepancy remains unresolved.
- Received and browser-rendered WAV files are retained locally. No sustained tonal candidate above
  the detector's review threshold was found in these short runs. This does **not** establish good
  speech quality, exclude intermittent beeps, or replace listening to the recordings.
- The longer stress attempt became unusable when the local frontend/Java services were no longer
  reachable. It must not be counted as a model reliability result. Missing reports remain visible
  in the aggregator as unreported failures.

## Engineering changes from this investigation

Safe tool/delegation diagnostics now retain exception class and the innermost source location,
without exception messages, transcripts, credentials or arguments. Privacy tests cover this.
Reports check fresh grounded actions and actual routes/headings/trailer positioning rather than
only counting tool calls. Browser and wire clocks can be aligned using `browserTimeOrigin`.

The test harness now terminates its own complete uv/Python process group, bounds synthetic
utterance completion, and saves wire/backend evidence even if the page disappears. Process-group
cleanup was verified by starting and stopping an isolated backend and checking its port closed.
The earlier long run exposed these harness weaknesses. The follow-up above completed the
outstanding test invocations and recorded session failures with intact artifacts.

## Next investigation

### September 15 user reproduction: delegation without navigation

The user's 11:39 UTC session had two distinct outcomes: delegation 1 finished after 2908ms
with no domain tools or UI actions; delegation 2 completed `search_movies` in approximately
66ms but failed with `input_tokens_limit` before emitting navigation. This is a per-run
application budget, not evidence of provider downtime or an exhausted account balance.

The text backend was adding all MCP output schemas to every model request. Local read-only
tool discovery measured approximately 8,045 serialized schema characters for anonymous tools
and 15,066 for authenticated tools, before provider-specific formatting. The text runner now
omits these prompt copies while preserving typed catalog/receipt processing and permission gates.
Token limits remain unchanged. Budget failures now report actual cumulative usage; delegation
context logs contain lengths/timing only, and text without tools/actions is labeled `text_only`.

A single bounded live **backend-only** probe, "Please open the movie Forrest Gump for me.",
then produced `search_movies` and `open_movie`, using 10,171 input tokens and 73 output tokens
over three model requests in 4,350ms. It used the same configured Luna backend and Live key,
but no microphone, Live transcription or browser navigation. This verifies one successful
tool path under the unchanged 24,000-input-token budget; it does not establish the cause of
the first text-only delegation or resolve the remaining voice stress failures.
The follow-up passed `make verify-agent` (392 tests, lint, formatting, strict types, architecture
and deterministic evals), including usage-failure counters and payload-free context logging.
Frontend, Java and container gates were not repeated for this Python adapter-only follow-up.

1. Diagnose GPT-Live's rapid-command delegation that completed without tools/actions, separating
   input recognition, selected transcript fragments and backend interpretation. Prefer fixture
   classifications and deterministic replay to retaining raw conversation content.
2. Recheck Grok spoken interruptions and ungrounded navigation attempts. Preserve catalog grounding
   and mutation protections; a model promise alone must not trigger an invented movie destination.
3. Review received versus rendered WAVs and repeat the manual MacBook microphone/speaker/headphone
   matrix. Numeric clipping/tonal checks cannot establish naturalness or exclude intermittent beeps.
4. Decide separately whether the existing cumulative cost limits fit long sessions; they are
   intentional and were not increased to make the stress suite pass.

Live artifacts are intentionally gitignored under `frontend/test-results/voice-stress/`.
Authenticated mutations, device echo/noise and statistical repeated-run reliability remain
additional coverage work; these initial results are not an overall success-rate estimate.

## Verification

- `make verify-agent`: formatting, lint, strict types, import/architecture contracts, all 390 Python
  tests and deterministic evals passed after the September 15 fixes.
- Frontend `yarn lint`, `yarn typecheck`, `yarn test --maxWorkers=2`: passed, 419 tests.
- `make test-voice-stress`: all four deterministic browser tests passed.
- `./gradlew processResources`: passed. The metadata-touch check above verified the development
  restart exclusion against the running Java process. Full Java/container gates were not repeated:
  this follow-up changes no Java code or runtime image, only a development DevTools exclusion.
- Live cases and their remaining failures are reported above; green deterministic checks do not
  establish provider reliability or microphone/speaker audio quality.
- Nothing was committed or deployed.

### Commit checkpoint

The subsequent commit preparation passed `make verify-agent` in the development tree (392 tests)
and an isolated index export's Python suite (380 tests). The 12 benchmark tests and the separate
direct-voice playground remain uncommitted; the exported runtime builds and starts without them.
Frontend lint, all 419 unit tests, type checking, `yarn build` and all four deterministic browser
stress tests passed. An ARM64 image built from the exported index passed the existing read-only,
non-root container smoke check. No additional paid voice sessions or production deployment ran.
