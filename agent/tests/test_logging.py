from __future__ import annotations

import json
from typing import TYPE_CHECKING

import structlog

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
    assert event["request_id"] == "request-123"
    assert "prompt" not in event
