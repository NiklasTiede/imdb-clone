from __future__ import annotations

from typing import TYPE_CHECKING, Final

import structlog
from opentelemetry.trace import get_current_span

if TYPE_CHECKING:
    from structlog.typing import EventDict, WrappedLogger

SAFE_LOG_FIELDS: Final = frozenset(
    {
        "duration_ms",
        "environment",
        "error_code",
        "estimated_cost_usd",
        "event",
        "input_tokens",
        "level",
        "log_level",
        "method",
        "model",
        "outcome",
        "output_tokens",
        "request_id",
        "requests",
        "route",
        "service",
        "span_id",
        "status_code",
        "timestamp",
        "tool",
        "tool_calls",
        "trace_id",
        "version",
    }
)


def add_trace_context(
    _logger: WrappedLogger,
    _method_name: str,
    event_dict: EventDict,
) -> EventDict:
    """Correlate logs with the active span without creating Loki labels."""

    span_context = get_current_span().get_span_context()
    if span_context.is_valid:
        event_dict.setdefault("trace_id", f"{span_context.trace_id:032x}")
        event_dict.setdefault("span_id", f"{span_context.span_id:016x}")
    return event_dict


def retain_safe_event_fields(
    _logger: WrappedLogger,
    _method_name: str,
    event_dict: EventDict,
) -> EventDict:
    """Drop fields that have not been explicitly approved for application logs."""

    return {key: value for key, value in event_dict.items() if key in SAFE_LOG_FIELDS}


def configure_logging(*, json_output: bool) -> None:
    """Configure idempotent, payload-safe structured application logging."""

    renderer = (
        structlog.processors.JSONRenderer()
        if json_output
        else structlog.dev.ConsoleRenderer(colors=False)
    )
    processors = [
        structlog.contextvars.merge_contextvars,
        structlog.processors.add_log_level,
        structlog.processors.TimeStamper(fmt="iso", utc=True),
        add_trace_context,
        retain_safe_event_fields,
        renderer,
    ]
    structlog.configure(
        cache_logger_on_first_use=False,
        logger_factory=structlog.PrintLoggerFactory(),
        processors=processors,
        wrapper_class=structlog.make_filtering_bound_logger(20),
    )
