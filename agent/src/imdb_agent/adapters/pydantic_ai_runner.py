from __future__ import annotations

import asyncio
from decimal import Decimal
from typing import TYPE_CHECKING, Self

import structlog
from openai import AsyncOpenAI
from pydantic import ValidationError
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
from pydantic_ai.models.openai import OpenAIResponsesModel, OpenAIResponsesModelSettings
from pydantic_ai.providers.openai import OpenAIProvider

from imdb_agent.adapters.application_tools import APPLICATION_TOOLS, ApplicationTools
from imdb_agent.adapters.catalog_contract import parse_grounded_movies
from imdb_agent.adapters.personal_tools import PersonalToolGate, base_toolset, personal_policy
from imdb_agent.concierge.events import (
    MovieCardEvent,
    OpenLoginAction,
    OpenWatchlistAction,
    RunnerEvent,
    TextEvent,
    ToolCallEvent,
    UiActionEvent,
    UsageEvent,
    UsageSummary,
)
from imdb_agent.concierge.personal import (
    PersonalTurn,
    pending_rating_target,
    receipt_action,
    requests_watchlist,
)
from imdb_agent.concierge.policy import (
    SYSTEM_POLICY,
    build_user_prompt,
    select_movies_for_display,
)
from imdb_agent.concierge.service import ConciergeRunError
from imdb_agent.concierge.streaming import StreamingRegion
from imdb_agent.concierge.tools import PERSONAL_TOOLS, ToolName

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


class PydanticAIConciergeRunner:
    """Pydantic AI/OpenAI/MCP Adapter behind the provider-neutral Concierge Interface."""

    def __init__(self, *, settings: Settings, secrets: RuntimeSecrets) -> None:
        self._settings: Settings | None = settings.model_copy(
            update={"mcp_bearer_token": secrets.mcp_bearer_token}
        )
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
        self._agent: Agent[None, str] = Agent(
            deps_type=type(None),
            model=model,
            instructions=SYSTEM_POLICY,
            model_settings=model_settings,
            output_type=str,
            retries=1,
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
        runner._settings = None
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
        personal = PersonalTurn(
            movies=next((m.movies for m in reversed(request.history) if m.movies), ())
        )
        personal.pending_rating_id = pending_rating_target(
            next((m.content for m in reversed(request.history) if m.role == "user"), ""),
            personal.movies,
        )
        personal.finalize(request.message)
        active_agent = self._agent
        if self._settings is not None:
            toolset = base_toolset(self._settings)
            toolset.process_tool_call = PersonalToolGate(
                request.delegation,
                personal,
                StreamingRegion(
                    request.page_context.streaming_country if request.page_context else "CH"
                ),
            ).call
            allowed = {name.value for name in ToolName}
            if request.delegation is None:
                allowed -= PERSONAL_TOOLS
            active_agent = Agent(
                deps_type=type(None),
                model=self._agent.model,
                model_settings=self._agent.model_settings,
                instructions=SYSTEM_POLICY + "\n" + personal_policy(request.delegation is not None),
                toolsets=[toolset.filtered(lambda _ctx, tool: tool.name in allowed)],
                retries=1,
            )
            active_agent.instrument = False
        application = ApplicationTools(
            personal,
            authenticated=request.delegation is not None,
            page_context=request.page_context,
        )
        action_sent = False
        search_navigation = application.search
        shown_movie_ids: set[int] = set()
        tool_arguments: dict[str, dict[str, object]] = {}
        try:
            async with (
                asyncio.timeout(self._run_timeout_seconds),
                active_agent.run_stream_events(
                    build_user_prompt(request.message, request.history),
                    conversation_id=request.conversation_id,
                    usage_limits=self._usage_limits,
                    toolsets=[application.toolset],
                ) as events,
            ):
                async for event in events:
                    if isinstance(event, FunctionToolCallEvent):
                        if event.part.tool_name in APPLICATION_TOOLS:
                            continue
                        search_navigation.started(event.tool_call_id)
                        if event.args_valid is not True:
                            continue
                        tool_name = _tool_name(event.part.tool_name)
                        arguments: dict[str, object] = event.part.args_as_dict()
                        tool_arguments[event.tool_call_id] = arguments
                        yield ToolCallEvent(tool=tool_name, arguments=arguments)
                    elif isinstance(event, FunctionToolResultEvent):
                        if (
                            isinstance(event.part, ToolReturnPart)
                            and event.part.outcome != "failed"
                        ):
                            if event.part.tool_name in APPLICATION_TOOLS:
                                continue
                            tool_name = _tool_name(event.part.tool_name)
                            if personal.receipt is not None and not action_sent:
                                action_sent = True
                                yield UiActionEvent(action=receipt_action(personal.receipt))
                            elif (
                                personal.watchlist_read
                                and requests_watchlist(request.message)
                                and not action_sent
                            ):
                                action_sent = True
                                yield UiActionEvent(action=OpenWatchlistAction())
                            movies = parse_grounded_movies(tool_name, event.part.content)
                            if tool_name not in {
                                ToolName.GET_MOVIE_ENRICHMENT,
                                ToolName.GET_MOVIE_WATCH_PROVIDERS,
                            }:
                                personal.remember_movies(movies)
                            search_navigation.succeeded(
                                event.tool_call_id,
                                tool_name,
                                tool_arguments.get(event.tool_call_id, {}),
                            )
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
                        if application.action is not None and not action_sent:
                            if application.movie is not None:
                                yield MovieCardEvent(movie=application.movie)
                            action_sent = True
                            yield UiActionEvent(action=application.action)
                        search_action = search_navigation.action(request.message, completed=True)
                        if search_action is not None and not action_sent:
                            action_sent = True
                            yield UiActionEvent(action=search_action)
                        if request.delegation is None and requests_watchlist(request.message):
                            yield UiActionEvent(action=OpenLoginAction())
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
