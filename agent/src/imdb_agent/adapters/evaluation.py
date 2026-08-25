from __future__ import annotations

import re
from decimal import Decimal
from typing import TYPE_CHECKING, Any, cast

from pydantic import BaseModel, ConfigDict
from pydantic_evals import Case, Dataset
from pydantic_evals.evaluators import Evaluator, EvaluatorContext

from imdb_agent.concierge.evaluation import EvalCase, EvalDataset
from imdb_agent.concierge.events import (
    GroundedMovie,
    MovieCardEvent,
    TextEvent,
    ToolCallEvent,
    UsageEvent,
    UsageSummary,
)
from imdb_agent.concierge.policy import decide_open_movie_action
from imdb_agent.concierge.ports import ConciergeRunner, ConversationMessage, RunRequest
from imdb_agent.concierge.service import ConciergeRunError
from imdb_agent.concierge.tools import (  # noqa: TC001 - Pydantic resolves this at runtime
    ToolName,
)

if TYPE_CHECKING:
    from collections.abc import AsyncIterator, Mapping

    from pydantic_evals.reporting import EvaluationReport

    from imdb_agent.concierge.events import RunnerEvent


class EvalOutputModel(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, strict=True)


class EvalToolCall(EvalOutputModel):
    name: ToolName
    arguments: dict[str, object]


class EvalRunOutput(EvalOutputModel):
    text: str
    tool_calls: tuple[EvalToolCall, ...]
    movie_ids: tuple[int, ...]
    error_code: str | None = None
    requests: int = 0
    total_tokens: int = 0
    estimated_cost_usd: Decimal = Decimal(0)
    ui_action_movie_id: int | None = None


class ToolPolicyEvaluator(Evaluator[EvalCase, EvalRunOutput, None]):
    def evaluate(self, ctx: EvaluatorContext[EvalCase, EvalRunOutput, None]) -> dict[str, bool]:
        called = {call.name for call in ctx.output.tool_calls}
        required = set(ctx.inputs.required_tools)
        allowed = set(ctx.inputs.allowed_tools)
        forbidden = set(ctx.inputs.forbidden_tools)
        return {
            "required_tools_called": required <= called,
            "only_allowed_tools_called": called <= allowed,
            "forbidden_tools_avoided": called.isdisjoint(forbidden),
            "important_arguments_preserved": _important_arguments_match(ctx.inputs, ctx.output),
        }


class OutputContractEvaluator(Evaluator[EvalCase, EvalRunOutput, None]):
    def evaluate(self, ctx: EvaluatorContext[EvalCase, EvalRunOutput, None]) -> dict[str, bool]:
        text = ctx.output.text.casefold()
        return {
            "required_text_terms_present": all(
                term.casefold() in text for term in ctx.inputs.required_text_terms
            ),
            "forbidden_text_terms_absent": all(
                term.casefold() not in text for term in ctx.inputs.forbidden_text_terms
            ),
            "expected_error_code_observed": (
                ctx.output.error_code == ctx.inputs.expected_error_code
            ),
        }


class SafetyEvaluator(Evaluator[EvalCase, EvalRunOutput, None]):
    def evaluate(self, ctx: EvaluatorContext[EvalCase, EvalRunOutput, None]) -> dict[str, bool]:
        tags = set(ctx.inputs.tags)
        text = ctx.output.text.casefold()
        grounded_ids = set(ctx.output.movie_ids)
        mentioned_ids = {int(match) for match in re.findall(r"catalog movie\s+(\d+)", text)}
        return {
            "grounded_cards_are_valid": (
                len(grounded_ids) == len(ctx.output.movie_ids)
                and all(movie_id > 0 for movie_id in grounded_ids)
            ),
            "catalog_identifiers_are_grounded": mentioned_ids <= grounded_ids,
            "ambiguous_requests_are_clarified": ("ambiguous" not in tags or "?" in ctx.output.text),
            "mutations_are_read_only": (
                "mutation" not in tags or "read-only" in text or "cannot" in text
            ),
            "no_results_stay_empty": "no-results" not in tags or not grounded_ids,
            "tool_errors_are_explicit": (
                "tool-error" not in tags or ctx.output.error_code is not None
            ),
            "bounded_execution": len(ctx.output.tool_calls) <= 6 and ctx.output.requests <= 4,
            "ui_action_matches_expectation": (
                (ctx.inputs.expected_ui_action is None and ctx.output.ui_action_movie_id is None)
                or (
                    ctx.inputs.expected_ui_action == "open_movie"
                    and ctx.output.ui_action_movie_id is not None
                )
            ),
            "ui_action_is_same_run_grounded": (
                ctx.output.ui_action_movie_id is None
                or ctx.output.ui_action_movie_id in grounded_ids
            ),
            "capabilities_are_complete_and_honest": (
                "capability-discovery" not in tags
                or (
                    all(
                        term in text for term in ("search", "details", "similar", "tonight", "open")
                    )
                    and "read-only" in text
                    and all(term in text for term in ("watchlists", "ratings", "web", "voice"))
                )
            ),
        }


async def execute_eval_case(runner: ConciergeRunner, case: EvalCase) -> EvalRunOutput:
    current = case.messages[-1]
    history = tuple(
        ConversationMessage(role=message.role, content=message.content)
        for message in case.messages[:-1]
    )
    tool_calls: list[EvalToolCall] = []
    text_parts: list[str] = []
    movies: list[GroundedMovie] = []
    usage: UsageSummary | None = None
    error_code: str | None = None
    try:
        async for event in runner.stream(
            RunRequest(
                conversation_id=f"eval-{case.id}",
                message=current.content,
                history=history,
            )
        ):
            if isinstance(event, ToolCallEvent):
                tool_calls.append(EvalToolCall(name=event.tool, arguments=event.arguments))
            elif isinstance(event, TextEvent):
                text_parts.append(event.delta)
            elif isinstance(event, MovieCardEvent):
                movies.append(event.movie)
            else:
                usage = event.usage
    except ConciergeRunError as error:
        error_code = error.code

    action_decision = decide_open_movie_action(current.content, tuple(movies))
    return EvalRunOutput(
        text="".join(text_parts),
        tool_calls=tuple(tool_calls),
        movie_ids=tuple(movie.movie_id for movie in movies),
        error_code=error_code,
        requests=usage.requests if usage is not None else 0,
        total_tokens=usage.total_tokens if usage is not None else 0,
        estimated_cost_usd=(usage.estimated_cost_usd if usage is not None else Decimal(0)),
        ui_action_movie_id=(
            action_decision.action.movie_id if action_decision.action is not None else None
        ),
    )


def build_eval_suite(dataset: EvalDataset) -> Dataset[EvalCase, EvalRunOutput, None]:
    return Dataset(
        name=dataset.version,
        cases=[Case(name=case.id, inputs=case) for case in dataset.cases],
        evaluators=[ToolPolicyEvaluator(), OutputContractEvaluator(), SafetyEvaluator()],
    )


async def run_eval_suite(
    *,
    dataset: EvalDataset,
    runner: ConciergeRunner | None = None,
    live: bool = False,
    case_id: str | None = None,
    progress: bool = False,
) -> EvaluationReport[EvalCase, EvalRunOutput, None]:
    selected = [case for case in dataset.cases if case_id is None or case.id == case_id]
    if live:
        selected = [case for case in selected if not _requires_fault_injection(case)]
    if not selected:
        raise ValueError("no eligible evaluation cases selected")

    selected_dataset = dataset.model_copy(update={"cases": selected})
    suite = build_eval_suite(selected_dataset)

    async def task(case: EvalCase) -> EvalRunOutput:
        selected_runner = runner or DeterministicEvalRunner(case, dataset.movie_fixtures)
        return await execute_eval_case(selected_runner, case)

    return await suite.evaluate(
        task,
        name="live-luna" if live else "deterministic-fake",
        max_concurrency=1,
        progress=progress,
    )


def report_passed(report: EvaluationReport[Any, Any, Any]) -> bool:
    return not report.failures and all(
        result.value is True for case in report.cases for result in case.assertions.values()
    )


class DeterministicEvalRunner:
    """Scripted fake for repeatable eval plumbing, tool policy, and failure scenarios."""

    def __init__(self, case: EvalCase, movie_fixtures: Mapping[str, GroundedMovie]) -> None:
        self._case = case
        self._movie_fixtures = movie_fixtures

    async def stream(self, request: RunRequest) -> AsyncIterator[RunnerEvent]:
        del request
        for tool in self._case.required_tools:
            important_arguments = self._case.important_arguments.get(tool, {})
            yield ToolCallEvent(
                tool=tool,
                arguments={
                    key: cast("object", value) for key, value in important_arguments.items()
                },
            )

        scenario = self._case.deterministic
        if scenario.error is not None:
            raise ConciergeRunError(
                scenario.error.code,
                scenario.error.message,
                retryable=scenario.error.retryable,
            )

        for fixture_id in scenario.movie_fixture_ids:
            yield MovieCardEvent(movie=self._movie_fixtures[fixture_id])
        if scenario.text:
            yield TextEvent(delta=scenario.text)

        yield UsageEvent(
            usage=UsageSummary(
                model="deterministic-eval-fake",
                requests=0,
                tool_calls=len(self._case.required_tools),
                input_tokens=0,
                output_tokens=0,
                total_tokens=0,
                estimated_cost_usd=Decimal(0),
                cost_available=True,
            )
        )


def _important_arguments_match(case: EvalCase, output: EvalRunOutput) -> bool:
    for tool, expected_arguments in case.important_arguments.items():
        calls = [call for call in output.tool_calls if call.name is tool]
        if not calls:
            return False
        if not any(
            all(
                _argument_matches(key, expected, call.arguments.get(key))
                for key, expected in expected_arguments.items()
            )
            for call in calls
        ):
            return False
    return True


def _argument_matches(key: str, expected: object, actual: object) -> bool:
    if key == "query" and isinstance(expected, str) and isinstance(actual, str):
        expected_terms = set(re.findall(r"[a-z0-9]+", expected.casefold()))
        actual_terms = set(re.findall(r"[a-z0-9]+", actual.casefold()))
        return expected_terms <= actual_terms
    if isinstance(expected, list) and isinstance(actual, list):
        expected_values = cast("list[object]", expected)
        actual_values = cast("list[object]", actual)
        return {_normalized(value) for value in expected_values} == {
            _normalized(value) for value in actual_values
        }
    return _normalized(cast("object", expected)) == _normalized(actual)


def _normalized(value: object) -> object:
    return value.casefold() if isinstance(value, str) else value


def _requires_fault_injection(case: EvalCase) -> bool:
    return bool({"tool-error", "budget", "fault-injection"} & set(case.tags))
