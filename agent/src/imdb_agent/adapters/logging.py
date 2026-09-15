from __future__ import annotations

from typing import TYPE_CHECKING, Final, cast

import structlog
from opentelemetry.trace import get_current_span

if TYPE_CHECKING:
    from structlog.typing import EventDict, WrappedLogger

SAFE_LOG_FIELDS: Final = frozenset(
    {
        "duration_ms",
        "delegation_sequence",
        "voice_model",
        "voice_seconds",
        "final_usage_confirmed",
        "backend_requests",
        "backend_tokens",
        "ui_actions",
        "session_limit_seconds",
        "phase",
        "audio_input_bytes",
        "audio_output_bytes",
        "control_messages",
        "typed_messages",
        "turns",
        "environment",
        "error_code",
        "error_type",
        "error_location",
        "error_chain",
        "budget",
        "estimated_cost_usd",
        "event",
        "input_tokens",
        "input_token_limit",
        "transcript_chars",
        "transcript_age_ms",
        "transcript_row_changed",
        "history_chars",
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


def usage_limit_budget(error: BaseException) -> str:
    """Classify SDK limits without copying unrestricted exception text into telemetry."""
    return next(
        (
            name
            for name in (
                "request_limit",
                "tool_calls_limit",
                "input_tokens_limit",
                "output_tokens_limit",
                "total_tokens_limit",
                "cost_limit",
            )
            if name in str(error)
        ),
        "unknown",
    )


def error_location(error: BaseException) -> str | None:
    """Code location only; exception messages and frame locals may contain credentials."""
    frame = error.__traceback__
    if frame is None:
        return None
    while frame.tb_next is not None:
        frame = frame.tb_next
    filename = frame.tb_frame.f_code.co_filename.rsplit("/", 1)[-1]
    return f"{filename}:{frame.tb_lineno}:{frame.tb_frame.f_code.co_name}"


def error_chain(error: BaseException) -> list[str]:
    """Bounded exception classes and code locations; never messages or frame locals."""
    result: list[str] = []
    seen: set[int] = set()
    pending = [error]
    while pending and len(result) < 8:
        current = pending.pop(0)
        if id(current) in seen:
            continue
        seen.add(id(current))
        location = f"{type(current).__name__}@{error_location(current)}"
        if isinstance(current, OSError) and isinstance(current.errno, int):
            location += f" errno={current.errno}"
        result.append(location)
        if isinstance(current, BaseExceptionGroup):
            pending[0:0] = cast("BaseExceptionGroup[BaseException]", current).exceptions
        else:
            cause = current.__cause__ or current.__context__
            if cause is not None:
                pending.insert(0, cause)
    return result


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
