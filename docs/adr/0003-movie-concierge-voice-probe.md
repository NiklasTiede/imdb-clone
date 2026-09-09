# ADR 0003: Prove realtime audio before integrating the voice channel

Date: 2026-09-09

Status: Accepted for the local compatibility probe; application voice transport remains proposed.

## Context

The current Concierge runner is a text-turn Interface. Native speech-to-speech requires a
bidirectional, long-lived session and cannot be added merely by replacing its model name.
The requested provider is xAI Grok Voice Think Fast 2.0. Live compatibility must be tested with
the exact framework/model combination before wiring browser audio and protected domain tools.

The initial voice experience uses English commands and replies, matching the predominantly English
movie catalog. Preserve catalog titles in tool arguments and answers. German dialogue and localized
title matching are outside this slice; future German commands still use English catalog titles.

## Decision

Use Pydantic AI 2.31.0's xAI realtime Adapter with `grok-voice-think-fast-2.0`, mono PCM16 at
24 kHz, manual input commit and one bounded session per opt-in CLI invocation. The probe owns
one synthetic tool and fixture; it imports no web code and accesses no application domain state.
The current fixture names Forrest Gump and returns a fixed test record; using a real film title
does not turn the simulated tool into a lookup in the user's library.
Provider types stay under `imdb_agent.adapters`; assembly is in the standalone probe CLI.
The existing text service does not load the xAI credential or start a voice connection.

Use a separate, explicit local xAI secret file. Keep instrumentation and provider logging off in
the CLI; report only allowlisted measurements and assertions. Retain just the latest two synthetic
reply WAVs and reports locally, outside Git. Tests use a scripted provider connection underneath
the real Pydantic AI tool/session loop, requiring no key or network.

Bound the entire session, input/output audio, model requests and tool execution. A wall-clock
deadline is essential: text token/cost limits alone do not bound a voice session. Report unavailable
cost estimates as null. Do not treat these controls as an account-wide dollar cap.

The next application slice should keep browser audio sessions in Python initially. Python will
execute Java-owned domain tools through protected MCP. Reuse the same application authorization
and action validation across text/voice; do not expose the MCP service credential to Grok or the
browser. Introduce the production voice Interface when implementing that actual channel, rather
than making this fixture probe a production agent.

## Consequences

- xAI cancellation is supported; output truncation to the played position is not. The probe
  verifies provider-confirmed interruption without sending the unsupported truncation command.
  Real playback flushing and human barge-in still need browser tests.
- This probe does not prove microphone permission handling, VAD, reconnect, page navigation,
  contextual application action interpretation or delegated watchlist access.
- The xAI extra adds its SDK/gRPC dependencies. The locked dependency resolver selects compatible
  packaging/protobuf versions; the complete existing agent gate and container smoke cover regressions.
- Audio replay latency is measured from connection start, not from a user's end of speech.
  Do not use this first-audio value as a production conversation latency target.

## Verification evidence

The English fixture passed live against the pinned model on 2026-09-09: correct movie lookup and
spoken title/runtime, then a separate run with a provider-confirmed interruption after lookup.
The provider sometimes speaks an acknowledgement before calling the tool, so the interruption
scenario waits for the lookup rather than cancelling that preamble. These are synthetic local
probe results, not evidence for real microphone handling or end-user tools.

## References

- [Pydantic AI xAI realtime](https://github.com/pydantic/pydantic-ai/blob/main/docs/realtime/xai.md)
- [xAI speech-to-speech](https://docs.x.ai/developers/model-capabilities/audio/speech-to-speech)
- [Delivery slice](../superpowers/plans/2026-09-09-movie-concierge-voice-slice.md)
