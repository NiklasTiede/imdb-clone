# Voice startup latency

Local inspection and measurement, 2026-09-09. No backend, provider SDK, deployment or key changes.

## Cause and change

Previously the browser awaited AudioContext.resume, then microphone permission/device acquisition,
then worklet loading, then session delegation and the browser/Python/Grok connection. Independent
waits accumulated. One observed cold startup spent 2,037 ms before getUserMedia even started;
the provider-ready event followed at 3,749 ms after the click.

BrowserAudio now initiates resume, microphone acquisition and worklet loading concurrently from
the click gesture. After microphone permission is granted, the hook starts its connection while
audio setup completes. It still waits for both usable audio and the server's ready event before
showing Listening or transmitting microphone data. No provider connection is opened while the
permission prompt is pending. Errors, cancellation and late permission results release resources;
late events cannot revive an ended session. Application events before full readiness are ignored.

Changed implementation:

- `frontend/src/features/concierge/audio/browserAudio.ts`
- `frontend/src/features/concierge/hooks/useConciergeVoice.ts`

## Measurement and limits

Three warm baseline trials reached the server-ready event at 1,388 / 1,438 / 1,308 ms after click.
Three final browser trials reached the actual Listening state at 1,669 / 1,487 / 1,236 ms. These
small samples overlap; they do not establish an overall percentage speedup. The change removes
serial asynchronous audio-device startup overhead rather than reducing provider network latency.
A more detailed final run measured about 241 ms in the synchronous AudioContext constructor on
its first start (this part still remains), followed by 27 ms in resume. Warm resume calls took
14–15 ms, overlapping with microphone acquisition. The original 2,037 ms observation was not
subdivided into constructor versus resume, so it cannot establish that this entire cold delay
has been eliminated.

An isolated Python startup measurement found 24–67 ms of setup before provider.connect, then
approximately 1,170–1,297 ms inside the Grok connection handshake. Opening a fresh remote session
therefore still has a substantial baseline cost. The benchmarks used anonymous sessions and a real
Chromium AudioContext with synthetic microphone input; signed-in starts also verify delegation.
No real microphone was recorded, no credentials were printed, and no provider session is speculatively
opened merely by opening the Concierge drawer. Tests that open actual sessions count against the
existing process budget; the local agent was restarted once after exhausting that budget.

Temporary benchmark scripts/results: `/tmp/imdb-voice-start-bench.mjs`,
`/tmp/imdb-voice-start-before.log`, `/tmp/imdb-voice-start-after.log`,
`/tmp/imdb-voice-server-bench.py`, `/tmp/imdb-voice-server-before.log`.

## Verification

- Focused audio/hook tests: 19 passed; final readiness guard: 10 hook tests passed.
- New tests cover parallel initialization, waiting for both readiness conditions, audio failure,
  cancellation, late permission grants and ignored premature status messages.
- Desktop/mobile E2E: 16 passed, including a controlled delayed AudioContext while the provider
  connection becomes ready. Existing capture/playback, permissions, navigation and personal actions
  also passed.
- Frontend lint and build passed; existing bundle-size/mixed-import warnings remain.
- `yarn test --maxWorkers=2`: full frontend suite passed, 344 tests in 106 files.
- Java/Python/container gates were not repeated: their source was not changed by this optimization.
