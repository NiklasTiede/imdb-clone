from enum import StrEnum
from typing import Literal, Self

from pydantic import BaseModel, ConfigDict, Field, JsonValue, model_validator


class StrictModel(BaseModel):
    """Base for immutable data validated at an external Seam."""

    model_config = ConfigDict(extra="forbid", frozen=True, strict=True)


class ToolName(StrEnum):
    SEARCH_MOVIES = "search_movies"
    GET_MOVIE_DETAILS = "get_movie_details"
    GET_SIMILAR_MOVIES = "get_similar_movies"
    GET_TONIGHT_PICKS = "get_tonight_picks"


class EvalMessage(StrictModel):
    role: Literal["user", "assistant"]
    content: str = Field(min_length=1)


class EvalCase(StrictModel):
    id: str = Field(min_length=1, pattern=r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
    messages: list[EvalMessage] = Field(min_length=1)
    required_tools: list[ToolName]
    allowed_tools: list[ToolName]
    forbidden_tools: list[ToolName]
    important_arguments: dict[ToolName, dict[str, JsonValue]]
    expected_behavior: list[str] = Field(min_length=1)
    forbidden_behavior: list[str] = Field(min_length=1)
    tags: list[str] = Field(min_length=1)

    @model_validator(mode="after")
    def validate_tool_policy(self) -> Self:
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
        return self


class EvalDataset(StrictModel):
    version: Literal["read-only-v1"]
    cases: list[EvalCase] = Field(min_length=1)

    @model_validator(mode="after")
    def validate_unique_case_ids(self) -> Self:
        case_ids = [case.id for case in self.cases]
        if len(case_ids) != len(set(case_ids)):
            raise ValueError("eval case IDs must be unique")
        return self
