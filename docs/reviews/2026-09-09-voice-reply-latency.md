# First voice reply latency

Local investigation on 2026-09-09, distinct from Start voice → Listening startup latency.

## Finding

The slow first response is reproducible with synthetic English speech through the actual
application WebSocket and Grok Voice Think Fast 2.0. For “Hello. What can you do?”, two baseline
sessions measured 3.262 / 3.678 seconds from the provider's speech-end event to the first PCM
packet. Repeating the question in the same session took 0.609 / 0.496 seconds. From the end of
the sent WAV fixture, the first packets arrived after 5.486 / 5.693 seconds; that larger interval
also includes speech-end detection, transcription and network transport. WAV end is an approximate
speech boundary, not a sample-accurate acoustic measurement.

A probe immediately around the SDK's provider codec measured 3.585 seconds for the first reply;
the application relay measured 3.586 seconds for the same turn. Thus the multi-second wait precedes
application audio forwarding. The SDK emits the first audio chunk as a SpeechPartDelta; its preceding
SpeechPart start is empty. No first chunk is lost by the relay. The browser additionally schedules
300 ms of playback lead, retained because it protects against the previously observed stuttering.
These measurements do not reveal xAI's internal cause; per-session model/audio initialization is a
hypothesis, not a confirmed server diagnosis.

The effect depends on the request. Starting a new session with “Please open Forrest Gump” produced
first audio 1.024 / 1.300 seconds after the provider speech-end event. Subsequent open commands were
not consistently faster. Baseline catalog calls usually took 20–64 ms, with a 237 ms outlier.
The tool and the spoken response are separate provider steps. One open trial did not produce a
navigation action; these timings are latency evidence, not a claim of perfect recognition/action
success. Existing grounding and authorization rules remain in force.

## Controlled experiments

Each temporary local server changed one factor; no alternative configuration was adopted.
Small samples show direction only, not p95 or production performance.

| Configuration | First capability reply, speech-end → PCM |
| --- | --- |
| Existing settings | 3.262 / 3.678 s |
| Reasoning disabled | 3.593 / 3.597 s |
| VAD silence 400 ms instead of 650 ms | 3.600 / 3.453 s |
| Short opening sentence and short capability response | 3.675 / 3.455 s |
| Omit tool return schemas from voice prompt | 3.739 / 3.765 s |

None removed the first-reply penalty. Shorter phrasing reduced reply length, but did not accelerate
its start. Reducing VAD silence did not establish a reliable improvement in the complete latency.
No speculative paid warm-up turn, fabricated user message, greeting, session-budget increase or
weaker grounding policy was introduced. Achieving a reliably fast first response remains open;
changing provider/session strategy requires a separately measured product decision.

SDK/provider references checked: installed PydanticAI 2.42.0 realtime session and xAI codec sources,
[PydanticAI realtime documentation](https://ai.pydantic.dev/realtime/), and
[xAI speech-to-speech parameters](https://docs.x.ai/developers/model-capabilities/audio/speech-to-speech).

## Delivered diagnostics and regression protection

- `adapters/voice_timing.py` records first versus later turn latency and individual tool durations
  using a monotonic clock. Only bounded event names, outcomes, tool names and durations are logged.
  Duplicate audio chunks do not generate duplicate measurements; interrupted turns discard pending
  tool measurements, and missing speech boundaries do not produce invented latency values.
- `adapters/realtime_voice.py` records audio timing after forwarding the first chunk. Logging
  failures cannot abort the voice stream. No browser protocol or domain contract changes.
- `evals/voice/replay_session.py` includes per-turn speech-end → first-PCM measurements in its
  opt-in report. Existing audio artifacts remain governed by the eval retention rules.
- Deterministic tests cover timing reset/deduplication, stale tool calls, missing boundaries and
  telemetry failures. A provider that withholds the remaining reply until the browser receives
  its first audio chunk proves that streaming does not wait for the whole answer.

Temporary synthetic benchmark scripts/results: `/tmp/imdb-reply-bench.py`,
`/tmp/imdb-reply-before.log`, `/tmp/imdb-reply-open-before.log`, and
`/tmp/imdb-reply-{fast,vad,concise,schema,codec}.log`. No real microphone recording or account
mutation was used. These files are local evidence and are not a permanent benchmark corpus.

## Verification

- `cd agent && uv run pytest tests/adapters/test_voice_timing.py tests/adapters/test_realtime_voice.py`:
  11 passed, including the first-chunk streaming regression.
- `make verify-agent`: passed formatting, Ruff, strict Pyright, 3 architecture tests, all 206
  application tests and the 27 deterministic eval cases.
- `cd agent && IMDB_AGENT_LIVE_EVALS_ENABLED=true uv run python evals/voice/replay_session.py --live --scenario open`:
  passed actual Grok/Java replay; grounded navigation 103 ms and first PCM 1.143 s after provider
  speech end. Runtime logs independently recorded 102 ms tool duration and 1.142 s first audio.
- `git diff --check`: passed.
- Java/frontend suites and image checks were not repeated: their contracts, implementation,
  dependencies and packaging were not changed by this investigation.
- Experimental servers on ports 8092–8096 were stopped. The ordinary local voice service on 8090
  was restarted with diagnostic events; its provider settings and playback behavior are unchanged.

The first-response performance requirement remains unresolved. This change provides measured
diagnosis and regression protection, not a demonstrated response-time improvement.
