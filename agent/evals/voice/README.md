# Synthetic voice compatibility fixture

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
