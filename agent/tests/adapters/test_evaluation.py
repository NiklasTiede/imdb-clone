from __future__ import annotations

from pathlib import Path
from typing import TYPE_CHECKING

import pytest

from imdb_agent.adapters.evaluation import report_passed, run_eval_suite
from imdb_agent.concierge.evaluation import load_eval_dataset
from imdb_agent.concierge.events import TextEvent
from imdb_agent.concierge.ports import ToolInvocation

if TYPE_CHECKING:
    from collections.abc import AsyncIterator

    from imdb_agent.concierge.events import RunnerEvent
    from imdb_agent.concierge.ports import RunRequest

AGENT_ROOT = Path(__file__).resolve().parents[2]

pytestmark = pytest.mark.asyncio


async def test_deterministic_eval_suite_passes_all_twenty_seven_cases() -> None:
    report = await run_eval_suite(
        dataset=load_eval_dataset(AGENT_ROOT / "evals" / "read_only_v1.json")
    )

    assert len(report.cases) == 27
    assert report_passed(report)


async def test_eval_suite_detects_a_forbidden_tool_call() -> None:
    class UnsafeRunner:
        async def stream(self, request: RunRequest) -> AsyncIterator[RunnerEvent]:
            assert request.trace_sink is not None
            request.trace_sink.record_tool_call(
                ToolInvocation(name="get_movie_details", arguments={"movieIds": [42]})
            )
            yield TextEvent(delta="unsafe")

    report = await run_eval_suite(
        dataset=load_eval_dataset(AGENT_ROOT / "evals" / "read_only_v1.json"),
        runner=UnsafeRunner(),
        case_id="exact-title-search",
    )

    assert not report_passed(report)
    assertions = report.cases[0].assertions
    assert assertions["forbidden_tools_avoided"].value is False
    assert assertions["only_allowed_tools_called"].value is False
