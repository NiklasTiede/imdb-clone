# Synthetic voice compatibility fixture

Application replay additionally uses two synthetic Samantha recordings in the same PCM format:

- `forrest-gump-open-en.wav`: “Please find Forrest Gump and open its movie page.”
- `open-it-en.wav`: “Open it, please.”

`replay_session.py` checks either the direct open command or the original runtime question followed
by “Open it” through the real application WebSocket and Java catalog. Run it only with `--live`
and `IMDB_AGENT_LIVE_EVALS_ENABLED=true`; see the agent README for limits and artifact retention.
Reports include `first_audio_after_speech_end_seconds` keyed by turn. This starts at the
provider's speech-end event and ends at the first received PCM packet; microphone/VAD delay and
the browser's playback buffer are excluded. Compare first and subsequent turns separately.
Runtime logs expose `voice_first_audio` with `first_turn`/`later_turn` and
`voice_tool_completed` with the tool name, outcome and duration in milliseconds. They contain
no transcripts, arguments, movie/account IDs or credentials.

`forrest-gump-en.wav` is synthetic English speech generated locally on macOS with the stock Samantha
voice. It contains no user recording or production data. PCM16, mono, 24 kHz; the probe rejects
other formats, truncated files, and recordings outside 0.1–15 seconds before connecting.

Utterance: “Please find the movie Forrest Gump in the test catalog and tell me its runtime.”

Regenerate from the repository root (the installed OS voice may change the resulting bytes):

```bash
say -v Samantha -r 160 -o agent/evals/voice/forrest-gump-en.wav \
  --file-format=WAVE --data-format=LEI16@24000 \
  'Please find the movie Forrest Gump in the test catalog and tell me its runtime.'
```

The fixture uses **Forrest Gump (1994)**, runtime **142 minutes**, matching the
[official Paramount film page](https://www.paramountpictures.com/movies/forrest-gump).
This is a fixed test record, not a live lookup in the application catalog.
Only `search_fixture_movie(title)` is available;
there is no Java/MCP connection, account, watchlist, or UI action in this probe.

Executable assertions live in `run_voice_probe` and are covered by deterministic protocol tests:

- Exactly one executed fixture lookup, with the expected title (case/whitespace normalized).
- Nonempty output audio and a completed tool/response exchange.
- In the normal scenario, the spoken transcript contains the fixture title and runtime.
- In the interrupt scenario, a cancellation is sent once the correct fixture lookup has executed
  and the configured amount of audio has arrived. Early spoken acknowledgements must not cancel
  the lookup before it runs. The provider must report an interrupted response. A request alone does
  not pass. This simulates client cancellation, not a human speaking over playback or server VAD.

Listen to the generated response for pronunciation, intelligibility and natural English; keyword
assertions do not prove these qualities. The initial corpus uses English commands and catalog titles.
German dialogue is deferred; future German commands must still use the English catalog titles.
