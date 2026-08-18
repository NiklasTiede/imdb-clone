from __future__ import annotations

from decimal import Decimal
from typing import TYPE_CHECKING

import pytest
from pydantic_ai import Agent
from pydantic_ai.models.function import AgentInfo, DeltaToolCall, FunctionModel

from imdb_agent.adapters.pydantic_ai_runner import (
    PydanticAIConciergeRunner,
    resolve_model_cost,
)
from imdb_agent.concierge.events import MovieCardEvent, TextEvent, UsageEvent
from imdb_agent.concierge.policy import SYSTEM_POLICY
from imdb_agent.concierge.ports import RunRequest

if TYPE_CHECKING:
    from collections.abc import AsyncIterator

    from pydantic_ai.messages import ModelMessage


@pytest.mark.asyncio
async def test_function_model_executes_tool_loop_and_emits_grounded_cards() -> None:
    calls = 0

    async def stream_function(
        messages: list[ModelMessage], info: AgentInfo
    ) -> AsyncIterator[str | dict[int, DeltaToolCall]]:
        nonlocal calls
        calls += 1
        if calls == 1:
            assert {tool.name for tool in info.function_tools} == {"search_movies"}
            yield {
                0: DeltaToolCall(
                    name="search_movies",
                    json_args='{"query":"Arrival"}',
                    tool_call_id="tool-call-1",
                )
            }
            return
        yield "Arrival is the grounded match."

    agent: Agent[None, str] = Agent(
        FunctionModel(stream_function=stream_function),
        instructions=SYSTEM_POLICY,
        output_type=str,
    )

    async def search_movies(query: str) -> dict[str, object]:
        assert query == "Arrival"
        return {
            "schemaVersion": "1.0",
            "movies": [
                {
                    "movieId": 42,
                    "primaryTitle": "Arrival",
                    "originalTitle": "Arrival",
                    "type": "MOVIE",
                    "startYear": 2016,
                    "runtimeMinutes": 116,
                    "genres": ["DRAMA", "SCI_FI"],
                    "imdbRating": 7.9,
                    "imdbRatingCount": 800000,
                    "description": "A grounded synopsis.",
                    "posterImageToken": "poster-token",
                }
            ],
            "totalMatches": 1,
            "moreAvailable": False,
        }

    agent.tool_plain(search_movies)

    runner = PydanticAIConciergeRunner.from_agent(agent=agent)
    events = [
        event
        async for event in runner.stream(
            RunRequest(
                conversation_id="conversation-1",
                message="Find Arrival.",
                history=(),
            )
        )
    ]

    assert calls == 2
    assert any(isinstance(event, MovieCardEvent) and event.movie.movie_id == 42 for event in events)
    assert "".join(event.delta for event in events if isinstance(event, TextEvent)) == (
        "Arrival is the grounded match."
    )
    usage = next(event.usage for event in events if isinstance(event, UsageEvent))
    assert usage.requests == 2
    assert usage.tool_calls == 1
    assert usage.input_tokens > 0
    assert usage.output_tokens > 0


def test_luna_cost_fallback_tracks_cached_and_uncached_tokens() -> None:
    cost, available, basis = resolve_model_cost(
        model_name="gpt-5.6-luna",
        provider_cost=None,
        input_tokens=1_000,
        cache_read_tokens=100,
        cache_write_tokens=50,
        output_tokens=200,
    )

    assert cost == Decimal("0.0004245")
    assert available is True
    assert basis == "openai-2026-07-30"
