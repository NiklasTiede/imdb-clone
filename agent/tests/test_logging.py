from __future__ import annotations

import json
from typing import TYPE_CHECKING

import structlog
from opentelemetry.trace import NonRecordingSpan, SpanContext, TraceFlags, use_span

from popcorn_society_agent.adapters.logging import configure_logging, error_chain, error_location

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
        error_type="UsageLimitExceeded",
        error_message="synthetic sensitive exception message",
    )

    event: dict[str, object] = json.loads(capsys.readouterr().out)
    assert event["event"] == "safe_event"
    assert event["level"] == "info"
    assert event["request_id"] == "request-123"
    assert "prompt" not in event
    assert event["error_type"] == "UsageLimitExceeded"
    assert "error_message" not in event


def test_live_diagnostics_survive_filter_without_conversation_payload(
    capsys: pytest.CaptureFixture[str],
) -> None:
    configure_logging(json_output=True)
    with structlog.contextvars.bound_contextvars(voice_model="gpt-live-1"):
        structlog.get_logger().info(
            "live_delegation_finished",
            delegation_sequence=2,
            outcome="backend_failed",
            error_code="tool_unavailable",
            tool_calls=1,
            ui_actions=0,
            transcript="private movie request",
            arguments={"token": "private-token"},
        )
        structlog.get_logger().info(
            "live_voice_usage",
            voice_seconds=24.0,
            final_usage_confirmed=True,
            backend_requests=2,
            backend_tokens=1200,
        )
    result, usage = [json.loads(line) for line in capsys.readouterr().out.splitlines()]
    assert result["voice_model"] == "gpt-live-1"
    assert result["delegation_sequence"] == 2
    assert result["outcome"] == "backend_failed"
    assert result["error_code"] == "tool_unavailable"
    assert result["tool_calls"] == 1 and result["ui_actions"] == 0
    assert "transcript" not in result and "arguments" not in result
    assert usage["voice_seconds"] == 24.0 and usage["final_usage_confirmed"] is True
    assert usage["backend_requests"] == 2 and usage["backend_tokens"] == 1200


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


def test_error_location_reports_only_source_not_exception_payload(
    capsys: pytest.CaptureFixture[str],
) -> None:
    configure_logging(json_output=True)
    assert error_location(RuntimeError("secret")) is None
    try:
        raise RuntimeError("synthetic private token")
    except RuntimeError as error:
        location = error_location(error)
        structlog.get_logger().warning(
            "mcp_tool_call_failed",
            error_type=type(error).__name__,
            error_location=location,
            exception=error,
        )
    output = capsys.readouterr().out
    event = json.loads(output)
    assert event["error_type"] == "RuntimeError"
    assert event["error_location"].startswith("test_logging.py:")
    assert event["error_location"].endswith(
        ":test_error_location_reports_only_source_not_exception_payload"
    )
    assert "synthetic private token" not in output
    assert "exception" not in event


def test_error_chain_never_contains_sensitive_messages() -> None:
    try:
        try:
            raise ConnectionError("private authentication header")
        except ConnectionError as cause:
            raise RuntimeError("private provider response") from cause
    except RuntimeError as error:
        chain = error_chain(error)
    assert len(chain) == 2
    assert chain[0].startswith("RuntimeError@test_logging.py:")
    assert chain[1].startswith("ConnectionError@test_logging.py:")
    assert "private" not in str(chain)
