"""Bounded provider compatibility probe, deliberately not a production Concierge runner."""

from __future__ import annotations

import asyncio
import re
import time
import wave
from dataclasses import dataclass, field
from typing import TYPE_CHECKING

from pydantic import BaseModel, ConfigDict, Field
from pydantic_ai import Agent
from pydantic_ai.exceptions import ModelHTTPError
from pydantic_ai.messages import ModelResponse, PartDeltaEvent, SpeechPart, SpeechPartDelta
from pydantic_ai.providers.xai import XaiProvider
from pydantic_ai.realtime import RealtimeTurnCompleteEvent
from pydantic_ai.realtime.xai import XaiRealtimeModelSettings
from pydantic_ai.usage import UsageLimits

from imdb_agent.adapters.xai_voice_model import ConciergeXaiVoiceModel

if TYPE_CHECKING:
    from pathlib import Path

    from pydantic_ai.realtime import RealtimeModel

    from imdb_agent.settings import LocalVoiceSecrets

VOICE_MODEL = "grok-voice-think-fast-2.0"
SAMPLE_RATE = 24_000
BYTES_PER_SECOND = SAMPLE_RATE * 2
MAX_INPUT_SECONDS = 15
MAX_OUTPUT_SECONDS = 20
CHUNK_BYTES = 4_800  # 100 ms of mono PCM16.


class VoiceProbeError(RuntimeError):
    """A content-free failure safe to display at the CLI boundary."""


class ProbeOptions(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, strict=True)

    session_seconds: float = Field(default=45.0, gt=0, le=60)
    interrupt_after_audio_ms: int | None = Field(default=None, ge=100, le=5_000)


class ProbeReport(BaseModel):
    """Only measurements and assertions; no audio, transcripts, keys or tool payloads."""

    model_config = ConfigDict(extra="forbid", frozen=True, strict=True)

    model: str
    passed: bool
    elapsed_seconds: float
    first_audio_seconds: float | None
    output_audio_seconds: float
    fixture_tool_calls: int
    fixture_arguments_match: bool
    turn_completed: bool
    interruption_requested: bool
    interruption_confirmed: bool
    fixture_reply_matches: bool
    input_tokens: int
    output_tokens: int
    estimated_cost_usd: str | None


@dataclass(frozen=True)
class ProbeResult:
    report: ProbeReport
    audio: bytes = field(repr=False)


def load_probe_audio(path: Path) -> bytes:
    """Validate format and duration before opening a billable connection."""
    if path.stat().st_size > BYTES_PER_SECOND * MAX_INPUT_SECONDS + 4_096:
        raise VoiceProbeError("input_audio_too_large")
    try:
        with wave.open(str(path), "rb") as audio:
            if (
                audio.getnchannels() != 1
                or audio.getsampwidth() != 2
                or audio.getframerate() != SAMPLE_RATE
                or audio.getcomptype() != "NONE"
            ):
                raise VoiceProbeError("input_requires_24000_hz_mono_pcm16_wav")
            frames = audio.getnframes()
            if not SAMPLE_RATE // 10 <= frames <= SAMPLE_RATE * MAX_INPUT_SECONDS:
                raise VoiceProbeError("input_audio_duration_out_of_bounds")
            data = audio.readframes(frames)
            if len(data) != frames * 2:
                raise VoiceProbeError("input_audio_truncated")
            return data
    except wave.Error, EOFError:
        raise VoiceProbeError("invalid_input_wav") from None


def build_voice_model(secrets: LocalVoiceSecrets) -> ConciergeXaiVoiceModel:
    return ConciergeXaiVoiceModel(
        VOICE_MODEL,
        provider=XaiProvider(api_key=secrets.xai_api_key.get_secret_value()),
        settings=XaiRealtimeModelSettings(
            xai_voice="eve",
            turn_detection=False,
            handshake_timeout=10.0,
            max_tokens=512,
            parallel_tool_calls=False,
        ),
    )


async def run_voice_probe(model: RealtimeModel, audio: bytes, options: ProbeOptions) -> ProbeResult:
    if not BYTES_PER_SECOND // 10 <= len(audio) <= BYTES_PER_SECOND * MAX_INPUT_SECONDS:
        raise VoiceProbeError("input_audio_duration_out_of_bounds")
    if len(audio) % 2:
        raise VoiceProbeError("invalid_pcm16_input")
    if (
        model.audio_input_sample_rate != SAMPLE_RATE
        or model.audio_output_sample_rate != SAMPLE_RATE
    ):
        raise VoiceProbeError("unsupported_provider_audio_rate")

    calls: list[bool] = []
    agent: Agent[None, str] = Agent(
        instructions=(
            "You are an English-speaking movie assistant in a synthetic audio test. "
            "Look up the movie the user names with search_fixture_movie. "
            "Preserve its English catalog title; do not translate movie titles. "
            "Use only the tool result and reply in one short English sentence with the "
            "title and runtime. There is no real user data, watchlist or navigation."
        ),
    )
    agent.instrument = False

    def search_fixture_movie(title: str) -> dict[str, str | int | bool]:
        """Look up a movie title in the synthetic test catalog, never the real catalog."""
        matched = title.strip().casefold() == "forrest gump"
        calls.append(matched)
        if not matched:
            return {"found": False}
        return {"found": True, "title": "Forrest Gump", "runtimeMinutes": 142}

    agent.tool_plain(search_fixture_movie)

    started = time.monotonic()
    first_audio: float | None = None
    output = bytearray()
    interrupted = False
    completed = False
    try:
        async with asyncio.timeout(options.session_seconds):
            async with agent.realtime(
                model,
                usage_limits=UsageLimits(
                    request_limit=3,
                    tool_calls_limit=2,
                    input_tokens_limit=8_000,
                    output_tokens_limit=2_000,
                ),
            ).session(audio_retention="transcript_only") as session:
                for offset in range(0, len(audio), CHUNK_BYTES):
                    await session.send_audio(audio[offset : offset + CHUNK_BYTES])
                await session.commit_audio()
                await session.create_response()
                async for event in session:
                    if (
                        isinstance(event, PartDeltaEvent)
                        and isinstance(event.delta, SpeechPartDelta)
                        and event.delta.speaker == "assistant"
                        and event.delta.audio_chunk
                    ):
                        chunk = event.delta.audio_chunk
                        if first_audio is None:
                            first_audio = time.monotonic() - started
                        if not interrupted:
                            if len(output) + len(chunk) > BYTES_PER_SECOND * MAX_OUTPUT_SECONDS:
                                raise VoiceProbeError("output_audio_limit_exceeded")
                            output.extend(chunk)
                            threshold = options.interrupt_after_audio_ms
                            if (
                                threshold is not None
                                and calls == [True]
                                and len(output) * 1_000 >= threshold * BYTES_PER_SECOND
                            ):
                                # Let the fixture lookup finish before cancelling. The provider
                                # may speak an acknowledgement before it invokes the tool.
                                # xAI supports cancellation, but not played_ms truncation.
                                await session.interrupt()
                                interrupted = True
                    elif isinstance(event, RealtimeTurnCompleteEvent):
                        completed = True
                        break
                usage = session.usage
                responses = [
                    message
                    for message in session.all_messages()
                    if isinstance(message, ModelResponse)
                ]
                interruption_confirmed = bool(responses) and responses[-1].state == "interrupted"
                reply = " ".join(
                    part.transcript or ""
                    for message in responses
                    for part in message.parts
                    if isinstance(part, SpeechPart) and part.speaker == "assistant"
                ).casefold()
                reply_matches = "forrest gump" in reply and (
                    re.search(r"\b(?:142|one hundred(?: and)? forty[ -]two)\b", reply) is not None
                )
    except TimeoutError:
        raise VoiceProbeError("voice_session_timeout") from None
    except VoiceProbeError:
        raise
    except ModelHTTPError as error:
        code = {
            401: "voice_authentication_failed",
            403: "voice_access_denied",
            429: "voice_rate_limited",
        }.get(error.status_code, "voice_provider_http_failure")
        raise VoiceProbeError(code) from None
    except Exception:
        # Provider exceptions may embed raw frames, transcripts or authorization details.
        raise VoiceProbeError("voice_provider_failure") from None

    arguments_match = calls == [True]
    expected_interruption = options.interrupt_after_audio_ms is not None
    passed = (
        bool(output)
        and completed
        and arguments_match
        and interrupted == expected_interruption
        and interruption_confirmed == expected_interruption
        and (expected_interruption or reply_matches)
    )
    return ProbeResult(
        report=ProbeReport(
            model=model.model_name,
            passed=passed,
            elapsed_seconds=round(time.monotonic() - started, 3),
            first_audio_seconds=round(first_audio, 3) if first_audio is not None else None,
            output_audio_seconds=round(len(output) / BYTES_PER_SECOND, 3),
            fixture_tool_calls=len(calls),
            fixture_arguments_match=arguments_match,
            turn_completed=completed,
            interruption_requested=interrupted,
            interruption_confirmed=interruption_confirmed,
            fixture_reply_matches=reply_matches,
            input_tokens=usage.input_tokens,
            output_tokens=usage.output_tokens,
            estimated_cost_usd=str(usage.cost) if usage.cost is not None else None,
        ),
        audio=bytes(output),
    )
