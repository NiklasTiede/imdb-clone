from __future__ import annotations

import asyncio
from decimal import Decimal
from typing import TYPE_CHECKING, Any, Literal, Self

import structlog
from openai import AsyncOpenAI
from pydantic import BaseModel, ConfigDict, Field, ValidationError
from pydantic_ai import (
    Agent,
    AgentRunResultEvent,
    FunctionToolCallEvent,
    FunctionToolResultEvent,
    PartDeltaEvent,
    PartStartEvent,
    TextPart,
    TextPartDelta,
    ToolReturnPart,
    UsageLimits,
)
from pydantic_ai.exceptions import (
    ModelHTTPError,
    RunCancelled,
    ToolFailed,
    UnexpectedModelBehavior,
    UsageLimitExceeded,
)
from pydantic_ai.mcp import MCPToolset
from pydantic_ai.models.openai import OpenAIResponsesModel, OpenAIResponsesModelSettings
from pydantic_ai.providers.openai import OpenAIProvider

from imdb_agent.concierge.events import (
    GroundedMovie,
    MovieCardEvent,
    RunnerEvent,
    TextEvent,
    ToolCallEvent,
    UsageEvent,
    UsageSummary,
)
from imdb_agent.concierge.policy import (
    SYSTEM_POLICY,
    build_user_prompt,
    select_movies_for_display,
)
from imdb_agent.concierge.service import ConciergeRunError
from imdb_agent.concierge.tools import ToolName

_LUNA_INPUT_PRICE_PER_MILLION = Decimal("0.20")
_LUNA_CACHED_INPUT_PRICE_PER_MILLION = Decimal("0.02")
_LUNA_CACHE_WRITE_PRICE_PER_MILLION = Decimal("0.25")
_LUNA_OUTPUT_PRICE_PER_MILLION = Decimal("1.20")
_TOKENS_PER_MILLION = Decimal(1_000_000)
_LUNA_PRICE_BASIS = "openai-2026-07-30"

if TYPE_CHECKING:
    from collections.abc import AsyncIterator

    from imdb_agent.concierge.ports import RunRequest
    from imdb_agent.settings import RuntimeSecrets, Settings


class _ToolModel(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, populate_by_name=True, strict=True)


class _ToolMovie(_ToolModel):
    movie_id: int = Field(alias="movieId", gt=0)
    primary_title: str = Field(alias="primaryTitle", min_length=1)
    original_title: str | None = Field(default=None, alias="originalTitle")
    type: str
    start_year: int | None = Field(default=None, alias="startYear")
    runtime_minutes: int | None = Field(default=None, alias="runtimeMinutes", ge=0)
    genres: list[str] = Field(default_factory=list)
    imdb_rating: float | None = Field(default=None, alias="imdbRating", ge=0, le=10)
    imdb_rating_count: int | None = Field(default=None, alias="imdbRatingCount", ge=0)
    description: str | None = Field(default=None, max_length=600)
    poster_image_token: str | None = Field(default=None, alias="posterImageToken")
    explanation: str | None = Field(default=None, max_length=400)

    def to_grounded(self) -> GroundedMovie:
        return GroundedMovie(
            movie_id=self.movie_id,
            primary_title=self.primary_title,
            original_title=self.original_title,
            movie_type=self.type,
            start_year=self.start_year,
            runtime_minutes=self.runtime_minutes,
            genres=tuple(self.genres),
            imdb_rating=self.imdb_rating,
            imdb_rating_count=self.imdb_rating_count,
            description=self.description,
            poster_image_token=self.poster_image_token,
            explanation=self.explanation,
        )


class _SearchResult(_ToolModel):
    schema_version: Literal["1.0"] = Field(alias="schemaVersion")
    movies: list[_ToolMovie]
    total_matches: int = Field(alias="totalMatches", ge=0)
    more_available: bool = Field(alias="moreAvailable")


class _DetailsResult(_ToolModel):
    schema_version: Literal["1.0"] = Field(alias="schemaVersion")
    movies: list[_ToolMovie]
    missing_movie_ids: list[int] = Field(alias="missingMovieIds")


class _SimilarResult(_ToolModel):
    schema_version: Literal["1.0"] = Field(alias="schemaVersion")
    strategy: str
    movies: list[_ToolMovie]


class _TonightResult(_ToolModel):
    schema_version: Literal["1.0"] = Field(alias="schemaVersion")
    seed: str
    movies: list[_ToolMovie]


class PydanticAIConciergeRunner:
    """Pydantic AI/OpenAI/MCP Adapter behind the provider-neutral Concierge Interface."""

    def __init__(self, *, settings: Settings, secrets: RuntimeSecrets) -> None:
        api_key = secrets.openai_api_key.get_secret_value()
        openai_client = AsyncOpenAI(
            api_key=api_key,
            max_retries=1,
            timeout=settings.provider_timeout_seconds,
        )
        provider = OpenAIProvider(openai_client=openai_client)
        model = OpenAIResponsesModel(settings.model_name, provider=provider)
        model_settings = OpenAIResponsesModelSettings(
            openai_reasoning_effort="low",
            openai_reasoning_context="all_turns",
            max_tokens=settings.max_output_tokens,
            timeout=settings.provider_timeout_seconds,
        )
        toolset = MCPToolset(
            settings.mcp_url,
            headers={"Authorization": f"Bearer {secrets.mcp_bearer_token.get_secret_value()}"},
            include_return_schema=True,
            init_timeout=settings.mcp_init_timeout_seconds,
            max_retries=0,
            read_timeout=settings.mcp_read_timeout_seconds,
            tool_error_behavior="failed",
        )
        self._agent: Agent[None, str] = Agent(
            model=model,
            instructions=SYSTEM_POLICY,
            model_settings=model_settings,
            output_type=str,
            retries=1,
            toolsets=[toolset],
        )
        self._usage_limits = UsageLimits(
            cost_limit=settings.run_cost_limit_usd,
            input_tokens_limit=settings.max_input_tokens,
            output_tokens_limit=settings.max_output_tokens,
            request_limit=settings.max_model_requests,
            tool_calls_limit=settings.max_tool_calls,
            total_tokens_limit=settings.max_input_tokens + settings.max_output_tokens,
        )
        self._model_name = settings.model_name
        self._run_timeout_seconds = settings.run_timeout_seconds
        self._logger = structlog.get_logger()

    @classmethod
    def from_agent(
        cls,
        *,
        agent: Agent[None, str],
        model_name: str = "deterministic-test-model",
        run_timeout_seconds: float = 5,
        usage_limits: UsageLimits | None = None,
    ) -> Self:
        """Build the Adapter around an injected deterministic Pydantic AI Agent."""

        runner = cls.__new__(cls)
        runner._agent = agent
        runner._usage_limits = usage_limits or UsageLimits(
            request_limit=4,
            tool_calls_limit=6,
            total_tokens_limit=10_000,
        )
        runner._model_name = model_name
        runner._run_timeout_seconds = run_timeout_seconds
        runner._logger = structlog.get_logger()
        return runner

    async def stream(self, request: RunRequest) -> AsyncIterator[RunnerEvent]:
        shown_movie_ids: set[int] = set()
        tool_arguments: dict[str, dict[str, object]] = {}
        try:
            async with (
                asyncio.timeout(self._run_timeout_seconds),
                self._agent.run_stream_events(
                    build_user_prompt(request.message, request.history),
                    conversation_id=request.conversation_id,
                    usage_limits=self._usage_limits,
                ) as events,
            ):
                async for event in events:
                    if isinstance(event, FunctionToolCallEvent):
                        if event.args_valid is not True:
                            continue
                        tool_name = _tool_name(event.part.tool_name)
                        arguments: dict[str, object] = event.part.args_as_dict()
                        tool_arguments[event.tool_call_id] = arguments
                        yield ToolCallEvent(tool=tool_name, arguments=arguments)
                    elif isinstance(event, FunctionToolResultEvent):
                        if isinstance(event.part, ToolReturnPart):
                            tool_name = _tool_name(event.part.tool_name)
                            movies = _parse_grounded_movies(tool_name, event.part.content)
                            for movie in select_movies_for_display(
                                tool_name,
                                movies,
                                tool_arguments.get(event.tool_call_id, {}),
                            ):
                                if movie.movie_id not in shown_movie_ids:
                                    shown_movie_ids.add(movie.movie_id)
                                    yield MovieCardEvent(movie=movie)
                    elif isinstance(event, PartStartEvent) and isinstance(event.part, TextPart):
                        if event.part.content:
                            yield TextEvent(delta=event.part.content)
                    elif isinstance(event, PartDeltaEvent) and isinstance(
                        event.delta, TextPartDelta
                    ):
                        if event.delta.content_delta:
                            yield TextEvent(delta=event.delta.content_delta)
                    elif isinstance(event, AgentRunResultEvent):
                        run_usage = event.result.usage
                        estimated_cost, cost_available, cost_basis = resolve_model_cost(
                            model_name=self._model_name,
                            provider_cost=run_usage.cost,
                            input_tokens=run_usage.input_tokens,
                            cache_read_tokens=run_usage.cache_read_tokens,
                            cache_write_tokens=run_usage.cache_write_tokens,
                            output_tokens=run_usage.output_tokens,
                        )
                        yield UsageEvent(
                            usage=UsageSummary(
                                model=self._model_name,
                                requests=run_usage.requests,
                                tool_calls=run_usage.tool_calls,
                                input_tokens=run_usage.input_tokens,
                                cache_read_tokens=run_usage.cache_read_tokens,
                                cache_write_tokens=run_usage.cache_write_tokens,
                                output_tokens=run_usage.output_tokens,
                                total_tokens=(run_usage.input_tokens + run_usage.output_tokens),
                                estimated_cost_usd=estimated_cost,
                                cost_available=cost_available,
                                cost_basis=cost_basis,
                            )
                        )
        except TimeoutError:
            self._logger.error("agent_run_failed", error_code="run_timeout")
            raise ConciergeRunError(
                "run_timeout",
                "The Movie Concierge took too long. Please try a narrower request.",
                retryable=True,
            ) from None
        except UsageLimitExceeded:
            self._logger.error("agent_run_failed", error_code="usage_limit")
            raise ConciergeRunError(
                "usage_limit",
                "The request reached its model or tool budget. Try a narrower request.",
                retryable=True,
            ) from None
        except ToolFailed:
            self._logger.error("agent_run_failed", error_code="tool_unavailable")
            raise ConciergeRunError(
                "tool_unavailable",
                "The movie catalog is temporarily unavailable.",
                retryable=True,
            ) from None
        except ModelHTTPError:
            self._logger.error("agent_run_failed", error_code="provider_unavailable")
            raise ConciergeRunError(
                "provider_unavailable",
                "The language model is temporarily unavailable.",
                retryable=True,
            ) from None
        except UnexpectedModelBehavior, ValidationError:
            self._logger.error("agent_run_failed", error_code="model_behavior")
            raise ConciergeRunError(
                "model_behavior",
                "The Movie Concierge could not produce a grounded answer.",
                retryable=True,
            ) from None
        except RunCancelled:
            raise asyncio.CancelledError from None


def _tool_name(value: str) -> ToolName:
    try:
        return ToolName(value)
    except ValueError:
        raise UnexpectedModelBehavior("Unknown MCP tool") from None


def _parse_grounded_movies(tool_name: ToolName, content: Any) -> tuple[GroundedMovie, ...]:
    if not isinstance(content, dict):
        raise UnexpectedModelBehavior("MCP tool returned non-object content")

    if tool_name is ToolName.SEARCH_MOVIES:
        result = _SearchResult.model_validate(content)
    elif tool_name is ToolName.GET_MOVIE_DETAILS:
        result = _DetailsResult.model_validate(content)
    elif tool_name is ToolName.GET_SIMILAR_MOVIES:
        result = _SimilarResult.model_validate(content)
    elif tool_name is ToolName.GET_TONIGHT_PICKS:
        result = _TonightResult.model_validate(content)
    return tuple(movie.to_grounded() for movie in result.movies)


def resolve_model_cost(
    *,
    model_name: str,
    provider_cost: Decimal | None,
    input_tokens: int,
    cache_read_tokens: int,
    cache_write_tokens: int,
    output_tokens: int,
) -> tuple[Decimal, bool, str | None]:
    if provider_cost is not None:
        return provider_cost, True, "pydantic-ai-provider-pricing"
    if model_name != "gpt-5.6-luna":
        return Decimal(0), False, None

    uncached_input_tokens = max(0, input_tokens - cache_read_tokens - cache_write_tokens)
    estimated = (
        Decimal(uncached_input_tokens) * _LUNA_INPUT_PRICE_PER_MILLION
        + Decimal(cache_read_tokens) * _LUNA_CACHED_INPUT_PRICE_PER_MILLION
        + Decimal(cache_write_tokens) * _LUNA_CACHE_WRITE_PRICE_PER_MILLION
        + Decimal(output_tokens) * _LUNA_OUTPUT_PRICE_PER_MILLION
    ) / _TOKENS_PER_MILLION
    return estimated, True, _LUNA_PRICE_BASIS
