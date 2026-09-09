from __future__ import annotations

from typing import TYPE_CHECKING

import structlog
from structlog.testing import capture_logs

from imdb_agent.adapters.voice_timing import VoiceTiming
from imdb_agent.concierge.tools import ToolName

if TYPE_CHECKING:
    import pytest


def test_first_audio_is_measured_once_per_turn_from_speech_end() -> None:
    now = 10.0
    timing = VoiceTiming(lambda: now)
    with capture_logs() as logs:
        timing.begin()
        timing.speech_ended()
        now = 10.5
        timing.speech_ended()  # Duplicate boundary must not move the baseline.
        now = 11.5
        timing.audio_sent()
        now = 12.0
        timing.audio_sent()
        timing.begin()
        timing.speech_ended()
        now = 12.25
        timing.audio_sent()
    assert [(item["duration_ms"], item["outcome"]) for item in logs] == [
        (1500, "first_turn"),
        (250, "later_turn"),
    ]
    assert all(item["event"] == "voice_first_audio" for item in logs)


def test_tool_timings_drop_stale_calls_and_log_only_bounded_fields() -> None:
    now = 0.0
    timing = VoiceTiming(lambda: now)
    with capture_logs() as logs:
        timing.begin()
        timing.tool_started("stale-private-id", ToolName.SEARCH_MOVIES)
        timing.begin()
        timing.tool_finished("stale-private-id", failed=False)
        timing.tool_started("private-id", ToolName.GET_MOVIE_DETAILS)
        now = 0.125
        timing.tool_finished("private-id", failed=True)
        timing.tool_finished("private-id", failed=True)
    assert logs == [
        {
            "event": "voice_tool_completed",
            "duration_ms": 125,
            "tool": "get_movie_details",
            "outcome": "failed",
            "log_level": "info",
        }
    ]


def test_missing_speech_boundary_does_not_invent_a_latency() -> None:
    timing = VoiceTiming()
    with capture_logs() as logs:
        timing.begin()
        timing.audio_sent()
        timing.speech_ended()
        timing.audio_sent()
    assert logs == []


def test_logging_failure_does_not_break_audio(monkeypatch: pytest.MonkeyPatch) -> None:
    def unavailable() -> None:
        raise RuntimeError("synthetic logging failure")

    timing = VoiceTiming()
    timing.begin()
    timing.speech_ended()
    monkeypatch.setattr(structlog, "get_logger", unavailable)
    timing.audio_sent()
