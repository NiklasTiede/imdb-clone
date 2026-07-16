from __future__ import annotations

from typing import TYPE_CHECKING, Final

import structlog

if TYPE_CHECKING:
    from structlog.typing import EventDict, WrappedLogger

SAFE_LOG_FIELDS: Final = frozenset(
    {
        "duration_ms",
        "environment",
        "error_code",
        "event",
        "log_level",
        "method",
        "outcome",
        "request_id",
        "route",
        "service",
        "status_code",
        "timestamp",
        "version",
    }
)


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
        retain_safe_event_fields,
        renderer,
    ]
    structlog.configure(
        cache_logger_on_first_use=False,
        logger_factory=structlog.PrintLoggerFactory(),
        processors=processors,
        wrapper_class=structlog.make_filtering_bound_logger(20),
    )
