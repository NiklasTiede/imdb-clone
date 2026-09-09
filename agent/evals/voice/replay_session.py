"""Opt-in synthetic audio replay through the actual application WebSocket and Java tools."""

from __future__ import annotations

import argparse
import asyncio
import json
import logging
import time
import wave
from pathlib import Path

from websockets.asyncio.client import connect

from imdb_agent.adapters.voice_probe import load_probe_audio
from imdb_agent.concierge.voice import PCM_BYTES_PER_SECOND, VoiceEvent
from imdb_agent.settings import DeploymentEnvironment, load_settings


async def replay(port: int, scenario: str) -> dict[str, object]:
    started = time.monotonic()
    fixture_dir = Path(__file__).parent
    names = (
        ["forrest-gump-open-en.wav"]
        if scenario == "open"
        else ["forrest-gump-en.wav", "open-it-en.wav"]
    )
    fixtures = [load_probe_audio(fixture_dir / name) for name in names]
    output = bytearray()
    grounded: set[int] = set()
    reply_matches = False
    sent = 0
    action_turn: int | None = None
    action_at: float | None = None
    action_grounded = False
    completed_turns: set[int] = set()
    speech_ends: dict[int, float] = {}
    first_audio: dict[int, float] = {}
    current_turn = 0
    catalog_ready: dict[int, float] = {}
    turn_audio_start = 0
    sender: asyncio.Task[None] | None = None

    async with (
        asyncio.timeout(55),
        connect(
            f"ws://127.0.0.1:{port}/v1/voice",
            origin="http://localhost:3000",
            max_size=1_048_576,
        ) as socket,
    ):
        await socket.send('{"type":"start"}')

        async def send_fixture(audio: bytes) -> None:
            # Real-time pacing exercises server VAD, including a trailing silence boundary.
            for offset in range(0, len(audio), 2400):
                await socket.send(audio[offset : offset + 2400])
                await asyncio.sleep(0.05)
            for _ in range(22):
                await socket.send(bytes(2400))
                await asyncio.sleep(0.05)

        try:
            async for message in socket:
                if isinstance(message, bytes):
                    if current_turn > 0:
                        first_audio.setdefault(current_turn, time.monotonic())
                    output.extend(message)
                    if len(output) > PCM_BYTES_PER_SECOND * 30:
                        raise ValueError("output limit")
                    continue
                event = VoiceEvent.model_validate_json(message)
                if event.turn > 0:
                    current_turn = event.turn
                now = time.monotonic()
                if event.type == "error":
                    raise ValueError("application voice error")
                if event.type == "ready":
                    sender = asyncio.create_task(send_fixture(fixtures[sent]))
                    sent += 1
                if (
                    event.type == "movie-card"
                    and event.movie
                    and event.movie.primary_title.casefold() == "forrest gump"
                ):
                    grounded.add(event.movie.movie_id)
                    catalog_ready.setdefault(event.turn, now)
                if event.type == "status" and event.status == "thinking":
                    speech_ends[event.turn] = now
                if event.type == "reply-complete":
                    completed_turns.add(event.turn)
                if event.type == "transcript" and event.speaker == "assistant":
                    reply_matches |= "forrest gump" in (event.text or "").casefold()
                if (
                    event.type == "status"
                    and event.status == "listening"
                    and reply_matches
                    and sent < len(fixtures)
                ):
                    if sender:
                        await sender
                    sender = asyncio.create_task(send_fixture(fixtures[sent]))
                    sent += 1
                    reply_matches = False
                    turn_audio_start = len(output)
                if event.type == "ui-action" and event.action:
                    action_turn = event.turn
                    action_at = now
                    action_grounded = event.action.movie_id in grounded
                # Navigation now precedes speech. Validate both outcomes independently.
                if action_turn is not None and action_turn in completed_turns:
                    passed = action_grounded and len(output) > turn_audio_start and reply_matches
                    await socket.send('{"type":"end"}')
                    artifacts = fixture_dir.parents[1] / ".artifacts" / "voice-probe"
                    artifacts.mkdir(parents=True, exist_ok=True)
                    with wave.open(str(artifacts / f"application-{scenario}.wav"), "wb") as audio:
                        audio.setparams((1, 2, 24000, 0, "NONE", "not compressed"))
                        audio.writeframes(output)
                    report: dict[str, object] = {
                        "passed": passed,
                        "scenario": scenario,
                        "grounded_action": action_grounded,
                        "reply_matches": reply_matches,
                        "output_audio_seconds": round(len(output) / PCM_BYTES_PER_SECOND, 2),
                        "elapsed_seconds": round(time.monotonic() - started, 2),
                        "first_audio_after_speech_end_seconds": {
                            str(turn): round(first_audio[turn] - ended, 3)
                            for turn, ended in speech_ends.items()
                            if turn in first_audio
                        },
                        "navigation_after_speech_end_seconds": (
                            round(action_at - speech_ends[action_turn], 3)
                            if action_at is not None and action_turn in speech_ends
                            else None
                        ),
                        "navigation_after_grounding_seconds": (
                            round(action_at - catalog_ready[action_turn], 3)
                            if action_at is not None and action_turn in catalog_ready
                            else None
                        ),
                    }
                    (artifacts / f"application-{scenario}.json").write_text(
                        json.dumps(report) + "\n"
                    )
                    return report
        finally:
            if sender:
                sender.cancel()
                await asyncio.gather(sender, return_exceptions=True)
    raise ValueError("incomplete voice session")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--live", action="store_true")
    parser.add_argument("--port", type=int, default=8090)
    parser.add_argument("--scenario", choices=["open", "context"], default="open")
    args = parser.parse_args()
    settings = load_settings()
    if (
        not args.live
        or not settings.live_evals_enabled
        or settings.environment is not DeploymentEnvironment.LOCAL
    ):
        print("Requires --live and IMDB_AGENT_LIVE_EVALS_ENABLED=true in local mode.")
        return 2
    logging.disable(logging.CRITICAL)
    try:
        report = asyncio.run(replay(args.port, args.scenario))
        print(json.dumps(report))
        return 0 if report["passed"] else 1
    except Exception:
        print('{"passed":false,"error":"voice_application_replay_failed"}')
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
