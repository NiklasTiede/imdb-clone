from __future__ import annotations

from decimal import Decimal
from typing import TYPE_CHECKING

import pytest
from pydantic_ai import Agent
from pydantic_ai.exceptions import ToolFailed
from pydantic_ai.models.function import AgentInfo, DeltaToolCall, FunctionModel

from imdb_agent.adapters.pydantic_ai_runner import (
    PydanticAIConciergeRunner,
    resolve_model_cost,
)
from imdb_agent.concierge.events import (
    MovieCardEvent,
    TextEvent,
    ToolCallEvent,
    UiActionEvent,
    UsageEvent,
)
from imdb_agent.concierge.policy import SYSTEM_POLICY
from imdb_agent.concierge.ports import RunRequest
from imdb_agent.concierge.tools import ToolName

if TYPE_CHECKING:
    from collections.abc import AsyncIterator

    from pydantic_ai.messages import ModelMessage


@pytest.mark.parametrize("message", ["Find Arrival.", "Find Arrival and open it."])
@pytest.mark.asyncio
async def test_function_model_executes_tool_loop_and_emits_grounded_cards(message: str) -> None:
    calls = 0

    async def stream_function(
        messages: list[ModelMessage], info: AgentInfo
    ) -> AsyncIterator[str | dict[int, DeltaToolCall]]:
        nonlocal calls
        calls += 1
        if calls == 1:
            assert {tool.name for tool in info.function_tools} == {
                "search_movies",
                "navigate_app",
                "open_movie_page",
                "show_movie_search",
            }
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
                },
                {
                    "movieId": 43,
                    "primaryTitle": "The Arrival",
                    "originalTitle": "The Arrival",
                    "type": "MOVIE",
                    "startYear": 1996,
                    "runtimeMinutes": 115,
                    "genres": ["SCI_FI"],
                    "imdbRating": 6.2,
                    "imdbRatingCount": 40000,
                    "description": "A nearby search candidate.",
                    "posterImageToken": None,
                },
            ],
            "totalMatches": 2,
            "moreAvailable": False,
        }

    agent.tool_plain(search_movies)

    runner = PydanticAIConciergeRunner.from_agent(agent=agent)
    events = [
        event
        async for event in runner.stream(
            RunRequest(
                conversation_id="conversation-1",
                message=message,
                history=(),
            )
        )
    ]

    actions = [event.action for event in events if isinstance(event, UiActionEvent)]
    if message == "Find Arrival.":
        assert len(actions) == 1 and actions[0].type == "show_search_results"
        assert actions[0].query == "Arrival"
    else:
        assert actions == []
    assert calls == 2
    tool_call = next(event for event in events if isinstance(event, ToolCallEvent))
    assert tool_call.tool is ToolName.SEARCH_MOVIES
    assert tool_call.arguments == {"query": "Arrival"}
    cards = [event.movie for event in events if isinstance(event, MovieCardEvent)]
    assert [movie.movie_id for movie in cards] == [42]
    assert "".join(event.delta for event in events if isinstance(event, TextEvent)) == (
        "Arrival is the grounded match."
    )
    usage = next(event.usage for event in events if isinstance(event, UsageEvent))
    assert usage.requests == 2
    assert usage.tool_calls == 1
    assert usage.input_tokens > 0
    assert usage.output_tokens > 0


@pytest.mark.asyncio
async def test_runner_ignores_rejected_tool_calls() -> None:
    calls = 0

    async def stream_function(
        messages: list[ModelMessage], info: AgentInfo
    ) -> AsyncIterator[str | dict[int, DeltaToolCall]]:
        nonlocal calls
        calls += 1
        if calls == 1:
            yield {
                0: DeltaToolCall(
                    name="search_movies",
                    json_args='{"unknown":"Arrival"}',
                    tool_call_id="rejected-tool-call",
                )
            }
            return
        if calls == 2:
            yield {
                0: DeltaToolCall(
                    name="search_movies",
                    json_args='{"query":"Arrival"}',
                    tool_call_id="accepted-tool-call",
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
            "movies": [],
            "totalMatches": 0,
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

    assert calls == 3
    tool_calls = [event for event in events if isinstance(event, ToolCallEvent)]
    assert tool_calls == [
        ToolCallEvent(
            tool=ToolName.SEARCH_MOVIES,
            arguments={"query": "Arrival"},
        )
    ]


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


@pytest.mark.asyncio
@pytest.mark.parametrize("followup", ["search_movies", "get_similar_movies", "failed"])
async def test_search_navigation_uses_final_successful_discovery(followup: str) -> None:
    calls = 0

    async def stream_function(
        messages: list[ModelMessage], info: AgentInfo
    ) -> AsyncIterator[str | dict[int, DeltaToolCall]]:
        nonlocal calls
        calls += 1
        if calls <= 2:
            name = (
                "get_similar_movies"
                if calls == 2 and followup == "get_similar_movies"
                else "search_movies"
            )
            arguments = (
                '{"movieId":42}'
                if name == "get_similar_movies"
                else ('{"query":"Forrest"}' if calls == 1 else '{"query":"Forrest Gump"}')
            )
            yield {0: DeltaToolCall(name=name, json_args=arguments, tool_call_id=f"lookup-{calls}")}
        else:
            yield "Here are the results."

    agent: Agent[None, str] = Agent(FunctionModel(stream_function=stream_function))

    async def search_movies(query: str) -> dict[str, object]:
        if query == "Forrest Gump" and followup == "failed":
            raise ToolFailed("Catalog unavailable")
        return {"schemaVersion": "1.0", "movies": [], "totalMatches": 0, "moreAvailable": False}

    async def get_similar_movies(movieId: int) -> dict[str, object]:
        return {"schemaVersion": "1.0", "movies": [], "strategy": "SIMILAR"}

    agent.tool_plain(search_movies)
    agent.tool_plain(get_similar_movies)
    runner = PydanticAIConciergeRunner.from_agent(agent=agent)
    events = [
        event
        async for event in runner.stream(
            RunRequest(
                conversation_id="navigation-review",
                message="Find movies similar to Forrest Gump"
                if followup == "get_similar_movies"
                else "Find Forrest Gump",
                history=(),
            )
        )
    ]
    actions = [event.action for event in events if isinstance(event, UiActionEvent)]
    if followup == "search_movies":
        assert len(actions) == 1 and actions[0].type == "show_search_results"
        assert actions[0].query == "Forrest Gump"
    else:
        assert not actions


@pytest.mark.asyncio
@pytest.mark.parametrize("movie_page", [False, True])
async def test_model_interpreted_navigation_is_emitted_without_command_regex(
    movie_page: bool,
) -> None:
    from pydantic import SecretStr

    from imdb_agent.concierge.events import GroundedMovie
    from imdb_agent.concierge.ports import ConversationMessage

    requests = 0

    async def stream_function(
        messages: list[ModelMessage], info: AgentInfo
    ) -> AsyncIterator[str | dict[int, DeltaToolCall]]:
        nonlocal requests
        requests += 1
        if requests == 1:
            yield {
                0: DeltaToolCall(
                    name="open_movie_page" if movie_page else "navigate_app",
                    json_args='{"movie_id":6}' if movie_page else '{"destination":"ratings"}',
                    tool_call_id="app-navigation",
                )
            }
        else:
            yield "Here you go."

    agent: Agent[None, str] = Agent(FunctionModel(stream_function=stream_function))
    runner = PydanticAIConciergeRunner.from_agent(agent=agent)
    movie = GroundedMovie(movie_id=6, primary_title="Forrest Gump", movie_type="MOVIE")
    events = [
        event
        async for event in runner.stream(
            RunRequest(
                conversation_id="semantic-navigation",
                message="Let's have a look at that one"
                if movie_page
                else "My rated movies, please",
                history=(
                    ConversationMessage(role="assistant", content="Forrest Gump", movies=(movie,)),
                ),
                delegation=SecretStr("synthetic-session"),
            )
        )
    ]
    actions = [event.action for event in events if isinstance(event, UiActionEvent)]
    assert len(actions) == 1
    assert actions[0].type == ("open_movie" if movie_page else "open_page")
    assert not any(isinstance(event, ToolCallEvent) for event in events)
    if movie_page:
        action_index = next(i for i, event in enumerate(events) if isinstance(event, UiActionEvent))
        assert isinstance(events[action_index - 1], MovieCardEvent)
