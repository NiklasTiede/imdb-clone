"""Payload-free timings for diagnosing slow first and subsequent voice replies."""

from __future__ import annotations

from contextlib import suppress
from time import monotonic
from typing import TYPE_CHECKING

import structlog

if TYPE_CHECKING:
    from collections.abc import Callable

    from imdb_agent.concierge.tools import ToolName


class VoiceTiming:
    def __init__(self, clock: Callable[[], float] = monotonic) -> None:
        self._clock = clock
        self._turn = 0
        self._speech_end: float | None = None
        self._audio_sent = False
        self._tools: dict[str, tuple[ToolName, float]] = {}

    def begin(self) -> None:
        self._turn += 1
        self._speech_end = None
        self._audio_sent = False
        self._tools.clear()

    def speech_ended(self) -> None:
        if self._speech_end is None:
            self._speech_end = self._clock()

    def audio_sent(self) -> None:
        if self._audio_sent:
            return
        self._audio_sent = True
        if self._speech_end is not None:
            self._record(
                "voice_first_audio",
                self._speech_end,
                outcome="first_turn" if self._turn == 1 else "later_turn",
            )

    def tool_started(self, call_id: str, name: ToolName) -> None:
        self._tools[call_id] = (name, self._clock())

    def tool_finished(self, call_id: str, *, failed: bool) -> None:
        pending = self._tools.pop(call_id, None)
        if pending is not None:
            name, started = pending
            self._record(
                "voice_tool_completed",
                started,
                tool=name.value,
                outcome="failed" if failed else "success",
            )

    def _record(self, event: str, started: float, **fields: str) -> None:
        # Telemetry failures must not break the audio stream.
        with suppress(Exception):
            structlog.get_logger().info(
                event, duration_ms=round((self._clock() - started) * 1000), **fields
            )
