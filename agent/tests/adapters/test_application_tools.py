from __future__ import annotations

import asyncio

import pytest
from pydantic_ai.exceptions import ToolFailed

from imdb_agent.adapters.application_tools import ApplicationTools
from imdb_agent.concierge.events import (
    GroundedMovie,
    OpenLoginAction,
    OpenMovieAction,
    OpenPageAction,
)
from imdb_agent.concierge.personal import PersonalTurn


@pytest.mark.asyncio
@pytest.mark.parametrize("authenticated", [False, True])
async def test_semantic_page_destination_respects_login(authenticated: bool) -> None:
    turn = PersonalTurn()
    turn.finalize("Let me see what I've rated so far")
    application = ApplicationTools(turn, authenticated=authenticated)
    await application.navigate_app("ratings")
    assert application.action == (
        OpenPageAction(destination="ratings") if authenticated else OpenLoginAction()
    )
    await application.navigate_app("home")
    assert application.action == OpenPageAction(destination="home")
    turn.begin()
    assert application.action is None


@pytest.mark.asyncio
async def test_open_contextual_movie_requires_catalog_evidence_and_active_turn() -> None:
    movie = GroundedMovie(movie_id=6, primary_title="Forrest Gump", movie_type="MOVIE")
    turn = PersonalTurn(movies=(movie,))
    turn.finalize("Let's have a look at that one")
    application = ApplicationTools(turn, authenticated=False)
    with pytest.raises(ToolFailed, match="catalog"):
        await application.open_movie_page(999)
    assert application.action is None
    await application.open_movie_page(6)
    assert application.action == OpenMovieAction(movie_id=6)
    assert application.movie == movie
    turn.cancelled = True
    assert application.action is None
    with pytest.raises(ToolFailed, match="no longer active"):
        await application.navigate_app("home")


@pytest.mark.asyncio
@pytest.mark.parametrize("interrupted", [False, True])
async def test_navigation_waits_for_current_final_transcript(interrupted: bool) -> None:
    turn = PersonalTurn()
    application = ApplicationTools(turn, authenticated=True)
    task = asyncio.create_task(application.navigate_app("settings"))
    await asyncio.sleep(0)
    assert not task.done()
    assert application.action is None
    if interrupted:
        turn.begin()
    turn.finalize("I'd like to change my account settings")
    if interrupted:
        with pytest.raises(ToolFailed, match="no longer active"):
            await task
        assert application.action is None
    else:
        await task
        assert application.action == OpenPageAction(destination="settings")


@pytest.mark.asyncio
async def test_semantic_search_uses_validated_parameters_and_drops_failed_refinement() -> None:
    from imdb_agent.concierge.tools import ToolName

    turn = PersonalTurn()
    turn.finalize("I feel like a short comedy")
    application = ApplicationTools(turn, authenticated=False)
    with pytest.raises(ToolFailed, match="catalog discovery"):
        await application.show_movie_search()
    application.search.started("discovery")
    application.search.succeeded(
        "discovery",
        ToolName.SEARCH_MOVIES,
        {"query": "", "genres": ["COMEDY"], "maxRuntimeMinutes": 90},
    )
    await application.show_movie_search()
    action = application.action
    assert action is not None and action.type == "show_search_results"
    assert action.genres == ["COMEDY"] and action.max_runtime_minutes == 90
    application.search.started("failed-refinement")
    assert application.action is None
    application.search.reset()
    assert application.action is None
