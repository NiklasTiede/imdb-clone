from __future__ import annotations

from typing import TYPE_CHECKING, Literal, Self

from pydantic import BaseModel, ConfigDict, Field, JsonValue, model_validator

from imdb_agent.concierge.events import (  # noqa: TC001 - Pydantic resolves this at runtime
    GroundedMovie,
)
from imdb_agent.concierge.tools import ToolName  # noqa: TC001 - Pydantic resolves this at runtime

if TYPE_CHECKING:
    from pathlib import Path


class EvalModel(BaseModel):
    """Immutable, strict data at the synthetic eval-dataset Seam."""

    model_config = ConfigDict(extra="forbid", frozen=True, strict=True)


class EvalMessage(EvalModel):
    role: Literal["user", "assistant"]
    content: str = Field(min_length=1)


class DeterministicEvalError(EvalModel):
    code: str = Field(min_length=1, max_length=80)
    message: str = Field(min_length=1, max_length=300)
    retryable: bool


class DeterministicEvalScenario(EvalModel):
    """Provider-free scripted output used to exercise the eval harness itself."""

    text: str = ""
    movie_fixture_ids: list[str] = Field(default_factory=list)
    error: DeterministicEvalError | None = None

    @model_validator(mode="after")
    def validate_unique_movie_fixtures(self) -> Self:
        if len(self.movie_fixture_ids) != len(set(self.movie_fixture_ids)):
            raise ValueError("deterministic movie fixture IDs must be unique")
        return self


class EvalCase(EvalModel):
    id: str = Field(min_length=1, pattern=r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
    messages: list[EvalMessage] = Field(min_length=1)
    required_tools: list[ToolName]
    allowed_tools: list[ToolName]
    forbidden_tools: list[ToolName]
    important_arguments: dict[ToolName, dict[str, JsonValue]]
    required_text_terms: list[str] = Field(default_factory=list)
    forbidden_text_terms: list[str] = Field(default_factory=list)
    expected_error_code: str | None = Field(default=None, min_length=1, max_length=80)
    expected_ui_action: Literal["open_movie"] | None = None
    review_criteria: list[str] = Field(min_length=1)
    review_risks: list[str] = Field(min_length=1)
    tags: list[str] = Field(min_length=1)
    deterministic: DeterministicEvalScenario

    @model_validator(mode="after")
    def validate_expectations(self) -> Self:
        required = set(self.required_tools)
        allowed = set(self.allowed_tools)
        forbidden = set(self.forbidden_tools)
        argument_tools = set(self.important_arguments)

        if not required <= allowed:
            raise ValueError("required tools must also be allowed")
        if allowed & forbidden:
            raise ValueError("allowed and forbidden tools must be disjoint")
        if not argument_tools <= allowed:
            raise ValueError("important arguments may only describe allowed tools")
        if len(required) != len(self.required_tools):
            raise ValueError("required tools must be unique")
        if len(allowed) != len(self.allowed_tools):
            raise ValueError("allowed tools must be unique")
        if len(forbidden) != len(self.forbidden_tools):
            raise ValueError("forbidden tools must be unique")
        if self.messages[-1].role != "user":
            raise ValueError("the final eval message must be a user request")
        if any(not term.strip() for term in self.required_text_terms):
            raise ValueError("required text terms must not be blank")
        if any(not term.strip() for term in self.forbidden_text_terms):
            raise ValueError("forbidden text terms must not be blank")
        scenario_error_code = (
            self.deterministic.error.code if self.deterministic.error is not None else None
        )
        if scenario_error_code != self.expected_error_code:
            raise ValueError("deterministic and expected error codes must match")
        return self


class EvalDataset(EvalModel):
    version: Literal["read-only-v1"]
    movie_fixtures: dict[str, GroundedMovie]
    cases: list[EvalCase] = Field(min_length=1)

    @model_validator(mode="after")
    def validate_dataset_references(self) -> Self:
        case_ids = [case.id for case in self.cases]
        if len(case_ids) != len(set(case_ids)):
            raise ValueError("eval case IDs must be unique")
        known_movie_fixtures = set(self.movie_fixtures)
        unknown_movie_fixtures = {
            fixture_id
            for case in self.cases
            for fixture_id in case.deterministic.movie_fixture_ids
            if fixture_id not in known_movie_fixtures
        }
        if unknown_movie_fixtures:
            raise ValueError(
                f"unknown deterministic movie fixtures: {sorted(unknown_movie_fixtures)}"
            )
        return self


def load_eval_dataset(path: Path) -> EvalDataset:
    """Load and strictly validate a synthetic, versioned eval dataset."""

    return EvalDataset.model_validate_json(path.read_text(encoding="utf-8"))
