from __future__ import annotations

from pathlib import Path
from typing import TYPE_CHECKING

import pytest

from imdb_agent.adapters.evaluation import report_passed, run_eval_suite
from imdb_agent.concierge.evaluation import load_eval_dataset
from imdb_agent.concierge.events import TextEvent, ToolCallEvent
from imdb_agent.concierge.tools import ToolName

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
            del request
            yield ToolCallEvent(
                tool=ToolName.GET_MOVIE_DETAILS,
                arguments={"movieIds": [42]},
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


async def test_eval_suite_enforces_machine_readable_text_expectations() -> None:
    dataset = load_eval_dataset(AGENT_ROOT / "evals" / "read_only_v1.json")
    case = next(case for case in dataset.cases if case.id == "exact-title-search")
    drifted_case = case.model_copy(
        update={
            "forbidden_text_terms": ["invented"],
            "deterministic": case.deterministic.model_copy(
                update={"text": "An invented catalog result."}
            ),
        }
    )

    report = await run_eval_suite(
        dataset=dataset.model_copy(update={"cases": [drifted_case]}),
    )

    assert not report_passed(report)
    assertions = report.cases[0].assertions
    assert assertions["required_text_terms_present"].value is False
    assert assertions["forbidden_text_terms_absent"].value is False


async def test_eval_suite_enforces_the_expected_error_code() -> None:
    dataset = load_eval_dataset(AGENT_ROOT / "evals" / "read_only_v1.json")
    case = next(case for case in dataset.cases if case.id == "mcp-search-timeout")
    assert case.deterministic.error is not None
    drifted_case = case.model_copy(
        update={
            "deterministic": case.deterministic.model_copy(
                update={
                    "error": case.deterministic.error.model_copy(update={"code": "model_behavior"})
                }
            )
        }
    )

    report = await run_eval_suite(
        dataset=dataset.model_copy(update={"cases": [drifted_case]}),
    )

    assert not report_passed(report)
    assert report.cases[0].assertions["expected_error_code_observed"].value is False
