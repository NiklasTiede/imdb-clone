# Voice reliability tests

These tests exercise the real React voice hook, microphone encoder/AudioWorklet, audio scheduler,
UI action grounding and routes. Synthetic English fixtures replace the physical microphone.
No personal storage state, real microphone recording or account mutations are used.

## Run

```bash
make test-voice-stress
# Paid: local Java backend and frontend; keys in the existing local secret files.
make test-voice-stress-live VOICE_STRESS_PROFILE=rapid  # two rapid commands, one final destination
make test-voice-stress-live VOICE_STRESS_PROFILE=quick
make test-voice-stress-live                       # stress profile
make test-voice-stress-live VOICE_STRESS_PROFILE=soak
make summarize-voice-stress
```

The ordinary Playwright suite excludes this directory. CI/free runs cannot connect to a provider;
live tests additionally require `VOICE_STRESS_LIVE=1`. There are no automatic retries, one worker,
and two model cases. Live sessions run in a separate agent process on port **8096**, using the same
app factory, provider adapters, prompts, Java MCP tools and settings as development. Port 8090 is
untouched. The test changes the browser WebSocket URL, not the audio protocol. It starts and stops
its own process, keeping only the application's allowlisted structured logs. Java remains on 8080,
frontend on 3000. The normal key files stay server-side.

| Profile | Requests per model | Hard session time cap per model |
| --- | ---: | ---: |
| rapid | 1 final destination (2 spoken commands) | 60 seconds |
| quick | 3 | 90 seconds |
| stress | 10 | 240 seconds |
| soak | 12 | 285 seconds |

These are maximum durations, not a minimum conversation length or a dollar budget. Tests stop
when their tasks finish or a session fails. Provider-side task/delegation quotas still apply;
incomplete sessions count as failures. Backend inference is billed separately. Never run paid
profiles in a retry loop; inspect evidence first. Select one provider with Playwright `--grep`
if needed. Before billing starts, the suite verifies local fixture availability and catalog titles.
During a live session, a one-second local TCP probe records catalog availability transitions.
After a detected outage the test stops further tasks and marks the run as infrastructure-interrupted;
this is not a provider reliability result. The probe does not assert database/search health.

## Scenarios and assertions

- Deterministic: an early greeting during delayed audio initialization, 12 consecutive movie/trailer
  navigations, duplicate actions, repeated interruptions of queued audio, burst delivery and jitter.
  Both model selections exercise the same provider-neutral client contract, including synthetic
  interrupt events. They do not simulate the providers' different full-duplex/VAD protocols or
  prove LLM intelligence. Real spoken interruption is covered only by the live cases.
- Live normal requests: exact fixture movie identity, a fresh grounded open action, actual movie
  route/heading, and trailer centering where requested. Spoken promises are insufficient.
- Live spoken interruption: first wait for audible browser output, then inject the spoken correction
  through the microphone stream. No fake `interrupt` event or client-cancel command is sent.
- Live rapid follow-ups: send two commands with a 100 ms gap; verify the final requested destination.
  Intermediate routes are diagnostic rather than mandatory. This is a latest-request navigation
  test, not an assertion that unrelated queued mutations may be discarded.
- Recovery: an audible output observation after the new input, no session error, and subsequent
  successful task execution. Audio after input may include a previous response tail; the report
  explicitly marks this attribution limitation. It is not a semantic answer-quality score.

## Evidence

Every run gets a unique directory under `frontend/test-results/voice-stress/`, which is gitignored.
Each scenario writes:

- `report.json`: timed, sanitized provider-neutral events, tool name/status, expected movie matches,
  UI outcome checks, actual microphone intervals, source start/stop schedule, queue/gap metrics,
  clipping, signal energy and sustained tonal candidates. Browser timestamps use `performance.now()`;
  add `browserTimeOrigin` to align them with epoch timestamps in wire/backend events.
- `received.wav`: provider PCM in arrival order (packet concatenation removes network silence).
- `rendered.wav`: a tap of the real Web Audio mix, including scheduled gaps and stopped sources.
- `backend.json` for live cases: correlated request IDs, delegation outcomes, tool timings and usage
  when reported. Logs contain no transcripts, tool arguments, credentials or raw exceptions.

The recorder is test-only and never shipped into the production audio path. Traces, screenshots
and video are disabled to avoid silently retaining protocol credentials or conversation payloads.
Keep only synthetic runs, and remove old run directories when no longer needed; there is no
background retention job. Regenerate fixtures on macOS with
`python3 agent/evals/voice/stress/generate.py` (Samantha, 24 kHz mono PCM16).

A packet gap does not prove audible stuttering: playback buffering may absorb it. A scheduling gap
may be natural silence. A tonal candidate may be intended sound. Listen to both WAVs before
attributing glitches to the provider or player. The audio tap does not measure physical speakers,
Bluetooth, microphone hardware, acoustic echo cancellation, intelligibility or naturalness.
Those still require a manual MacBook/Chrome and headphone/speaker matrix. Synthetic input does not
include trailer sound leaking into the microphone, accents, background noise or real user timing.
Authenticated watchlist/rating mutations and login/logout continuity remain separate coverage work.

Aggregates keep providers, profiles, report schema versions and catalog availability categories separate, include unattempted tasks in completion rates,
and report sample counts plus P50/P90 of successful task completion latency. Failures are not
silently removed from the success denominator. Runs that crashed before writing a report are listed
separately as `unreported_failed_runs`; group rates are incomplete while such failures exist. Repeated runs are necessary before comparing
reliability statistically; a single green run is not a production readiness claim.

This separation of task completion, interaction timing, consumption and human audio review follows
[OpenAI's voice evaluation guidance](https://developers.openai.com/cookbook/examples/audio/voice_agent_evaluation#metrics).

## Manual audio matrix after an automated regression fix

Use a fresh session for each combination of Grok/GPT-Live and MacBook speakers/headphones.
Start with a short greeting, interrupt a longer answer three times with different movie requests,
then open a trailer and speak over it. Check first syllables, lingering old speech, clipped words,
beeps, response recovery and the final visible movie. Include microphone denial and stop/restart.
Record the provider, browser, output device and approximate time so the safe backend timeline can
be correlated. Do not enable personal audio recording or transcript logging by default.
