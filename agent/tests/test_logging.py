from __future__ import annotations

import json
from typing import TYPE_CHECKING

import structlog
from opentelemetry.trace import NonRecordingSpan, SpanContext, TraceFlags, use_span

from imdb_agent.adapters.logging import configure_logging

if TYPE_CHECKING:
    import pytest


def test_json_logging_keeps_allowlisted_context_and_discards_payload(
    capsys: pytest.CaptureFixture[str],
) -> None:
    configure_logging(json_output=True)
    logger = structlog.get_logger()

    logger.info(
        "safe_event",
        request_id="request-123",
        prompt="do not log this prompt",
    )

    event: dict[str, object] = json.loads(capsys.readouterr().out)
    assert event["event"] == "safe_event"
    assert event["level"] == "info"
    assert event["request_id"] == "request-123"
    assert "prompt" not in event


def test_json_logging_adds_active_trace_context_without_payload(
    capsys: pytest.CaptureFixture[str],
) -> None:
    configure_logging(json_output=True)
    logger = structlog.get_logger()
    span = NonRecordingSpan(
        SpanContext(
            trace_id=0x1234,
            span_id=0x5678,
            is_remote=False,
            trace_flags=TraceFlags(TraceFlags.SAMPLED),
        )
    )

    with use_span(span, end_on_exit=False):
        logger.info("safe_event", prompt="never emit this")

    event: dict[str, object] = json.loads(capsys.readouterr().out)
    assert event["trace_id"] == "00000000000000000000000000001234"
    assert event["span_id"] == "0000000000005678"
    assert "prompt" not in event
